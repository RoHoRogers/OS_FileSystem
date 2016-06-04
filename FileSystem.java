// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

import java.util.*;

public class FileSystem 
{
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	public final int ERROR = -1;
	public final int OK = 0;
	public final int SEEK_SET = 0;
	public final int SEEK_CUR = 1;
	public final int SEEK_END = 2;

	// ------------------------ Constructor -------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public FileSystem(int blocks) 
	{
		byte[] dirData;
		superblock = new SuperBlock(blocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		// read the "/" file from disk
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if (dirSize > 0) 
		{
			// the directory has some data
			dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}

	// ------------------------ sync --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public void sync() 
	{
		// write the "/" file from disk
		FileTableEntry dirEnt = open("/", "w");
		// get directory data in bytes
		byte[] dirData = directory.directory2bytes();
		// write the file table entry
		write(dirEnt, dirData);
		close(dirEnt);
		// tell super block to write to the disk
		superblock.sync();
	}

	// ------------------------ format ------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public boolean format(int files) 
	{
		// file table contents must be closed
		if (!filetable.fempty()) 
		{
			// TODO: omit or wait with while loop if this causes errors
			Kernel.report("Format failure: Cannot format superblock while file are in use");
			return false;
		}
		superblock.format(files);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		return true;
	}

	// ------------------------ open --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public FileTableEntry open(String filename, String mode) {
		FileTableEntry fte;
		Inode iNode;
		// must provide valid filename and mode
		if (filename == "" || mode == "")
		{
			return null;
		}
		
		
		// if file table entry is null or mode is invalid
		// or iNode is null or iNode flag is 'to be deleted'
		if ((fte = filetable.falloc(filename, mode)) == null || fte.mode == -1
				|| (iNode = fte.iNode) == null || iNode.flag == Inode.DELETE) 
		{ 
				filetable.ffree(fte); // relieve entry from memory
				return null;
		}
		
		synchronized (fte) 
		{
			// if mode is "w" delete all blocks and write from scratch
			if (fte.mode == FileTableEntry.WRITEONLY && !deallocateBlocks(fte)) 
			{
				// on failure, relieve entry from memory
				filetable.ffree(fte);
				Kernel.report("Open failure: Could not deallocate all blocks");
				return null;
			}
		}
		return fte;
	}

	// Commits all file transactions on this file,
	// and unregisters fd from the user file descriptor table
	// of the calling thread's TCB. Returns success.
	// ------------------------ close -------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public boolean close(FileTableEntry fte) 
	{
		Inode iNode;
		if (fte == null)
		{
			return false;
		}

		synchronized (fte) 
		{
			if ((iNode = fte.iNode) == null)
			{
				return false;
			}
			if (iNode.flag == Inode.DELETE && fte.count == 0) 
			{
				// deallocate file table entry if no more threads are using the
				// file and it has been marked as deleted (could be due to a bad
				// write)
				deallocateBlocks(fte);
				if (!directory.ifree(fte.iNumber))
				{
					return false;
				}
			}
			if (!filetable.ffree(fte))
			{
				return false;
			}
		}
		return true;
	}

	// ------------------------ fsize -------------------------------------- //
	// Return size in bytes
	// --------------------------------------------------------------------- //
	public int fsize(FileTableEntry fte) 
	{
		Inode iNode;
		// return -1 if fte is null
		if (fte == null)
		{
			return ERROR;
		}
		// return -1 if iNode is null
		if ((iNode = fte.iNode) == null)
		{
			return ERROR;
		}
		// return iNode.length (already in bytes, no need for conversion)
		return iNode.length;
	}

	// ------------------------ read --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public int read(FileTableEntry fte, byte[] buffer) 
	{
		int seekPtr, length, block, offset, available, remaining, rLength, index;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null)
		{
			return ERROR;
		}
		// mode must be read
		if (fte.mode == FileTableEntry.WRITEONLY || fte.mode == FileTableEntry.APPEND)
		{
			return ERROR;
		}
		// iNode cannot be null
		if ((iNode = fte.iNode) == null)
		{
			return ERROR;
		}
		// read up to buffer length
		length = buffer.length;

		// multiple threads cannot read at the same time
		synchronized (fte) 
		{
			// start at position pointed to by iNode's seek pointer
			seekPtr = fte.seekPtr;
			data = new byte[Disk.blockSize];
			index = 0;
			while (index < length) 
			{
				// byte offset-- 0 is a new block
				offset = seekPtr % Disk.blockSize;
				// bytes available
				available = Disk.blockSize - offset;
				// bytes remaining
				remaining = length - index;
				// bytes to read-- cannot be greater than available
				rLength = Math.min(available, remaining);

				// block must exist
				if ((block = iNode.findTargetBlock(offset)) == ERROR) 
				{
					// if ((block = iNode.findTargetBlock(seekPtr)) == ERROR) {
					Kernel.report("Read failure: Failed to find target block "
							+ seekPtr + "\n");
					return ERROR;
				}

				if (block < 0 || block >= superblock.totalBlocks) 
				{
					Kernel.report("Read error: Block " + block
							+ " out of range\n");
					break;
					// return ERROR;
				}

				if (offset == 0) 
				{
					data = new byte[Disk.blockSize];
				}

				// read block from disk to data
				SysLib.rawread(block, data);

				// copy data to buffer
				// source, source position, destination, destination position,
				// length to copy
				System.arraycopy(data, offset, buffer, index, rLength);

				index += rLength;
				seekPtr += rLength;
			}
			// set new seek pointer
			seek(fte, index, SEEK_CUR);
		}
		return index;
	}


	/*
	 * TODO: corresponding bytes should be updated... hmmm ? "w" should actually
	 * delete all blocks first and then start writing from scratch. Contrary to
	 * "w", the "a" mode should keep all blocks, set the seek pointer to the
	 * EOF, and thereafter appends new blocks. "w+" should keep all blocks. If
	 * the seek pointer is within the EOF, the corresponding bytes should be
	 * updated. If the seek pointer is at the EOF, it behaves like "a".
	 */

	// ------------------------ write -------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public int write(FileTableEntry fte, byte[] buffer) 
	{
		int seekPtr, length, offset, remaining, available, wLength, index;
		short block;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null)
		{
			return ERROR;
		}		
		// mode cannot be read only
		if (fte.mode == FileTableEntry.READONLY)
		{
			return ERROR;
		}
		// iNode cannot be null
		if ((iNode = fte.iNode) == null)
		{
			return ERROR;
		}
		// iNode must not be in use
		if (iNode.flag == Inode.READ || iNode.flag == Inode.WRITE || iNode.flag == Inode.DELETE)
		{
			return ERROR;
		}
		
		// write up to buffer length
		length = buffer.length;
		// on error, set iNode flag to "to be deleted" because it's probably
		// garbage now
		// multiple threads cannot write at the same time
		synchronized (fte) 
		{
			// start at position pointed to by inode's seek pointer
			// append should set seek pointer to EOF
			seekPtr = fte.mode == FileTableEntry.APPEND
					? seek(fte, 0, SEEK_END)
					: fte.seekPtr;
			iNode.flag = Inode.WRITE; // set flag to write
			index = 0;
			data = new byte[Disk.blockSize];
			while (index < length) 
			{
				// byte offset-- 0 is a new block
				offset = seekPtr % Disk.blockSize;
				// bytes available
				available = Disk.blockSize - offset;
				// bytes remaining
				remaining = length - index;
				// bytes to write-- cannot be greater than available
				wLength = Math.min(available, remaining);

				// get next block from iNode
				if ((block = iNode.findTargetBlock(offset)) == ERROR) 
				{
					// if ERROR, file is out of memory, so get a new block
					if ((block = superblock.getFreeBlock()) == ERROR) 
					{
						Kernel.report("Write failure: Out of memory!");
						iNode.flag = Inode.DELETE;
						break;
						// return ERROR; // no more free blocks
					}
					// read the file to the block
					if (iNode.setTargetBlock(seekPtr, block) == ERROR) 
					{
						// out of bounds, try to get a new indirect block
						if (iNode.setIndexBlock(block) == false) 
						{
							Kernel.report("Write failure: Failed to set index block "
									+ block);
							iNode.flag = Inode.DELETE;
							break;
							// return ERROR;
						}
						// index block set, get a new block
						if ((block = superblock.getFreeBlock()) == ERROR) 
						{
							Kernel.report("Write failure: Out of memory!");
							iNode.flag = Inode.DELETE;
							break;
							// return ERROR; // no more free blocks
						}
						if (iNode.setTargetBlock(seekPtr, block) == ERROR) 
						{
							Kernel.report("Write failure: Failed to set target block "
									+ block);
							iNode.flag = Inode.DELETE;
							break;
							// return ERROR;
						}
					}
				}

				if (block >= superblock.totalBlocks) 
				{
					Kernel.report("Write failure: Block" + block
							+ " out of range");
					iNode.flag = Inode.DELETE;
					break;
				}

				if (offset == 0) 
				{
					data = new byte[Disk.blockSize];
				}

				SysLib.rawread(block, data);

				// copy data to buffer
				// source, source position, destination, destination position,
				// length to copy
				System.arraycopy(buffer, index, data, offset, wLength);
				// write data to disk

				SysLib.rawwrite(block, data);

				index += wLength;
				seekPtr += wLength;
			}
			// update iNode for append or w+
			if (seekPtr > iNode.length)
			{
				iNode.length = seekPtr;
			}
			// set new seek pointer
			seek(fte, index, SEEK_CUR);
			if (iNode.flag != Inode.DELETE) 
			{
				// iNode is now USED
				iNode.flag = Inode.USED;
			}
			// save iNode to disk
			iNode.toDisk(fte.iNumber);
		}
		// if error was not returned, all bytes wrote successfully-- return
		// length
		return index;
	}
	
	
	// ------------------------ delete ------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public boolean delete(String fname) 
	{
		int iNumber;
		if (fname == "") // if blank file name, return false
		{
			return false;
		}
		// get the iNumber for this filename
		if ((iNumber = directory.namei(fname)) == -1)
		{
			return false; // if it does not exist, return false
		}
		// deallocate file, return success or failure
		return directory.ifree(iNumber);
	}

	// ------------------------ seek --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public int seek(FileTableEntry fte, int offset, int whence) 
	{
		// seek pointer, end of file
		int seekPtr, EOF;
		if (fte == null)
		{
			return ERROR;
		}
		synchronized (fte) 
		{
			seekPtr = fte.seekPtr;
			EOF = fsize(fte);
			switch (whence) 
			{
				case SEEK_SET :
					// file's seek pointer is set to offset bytes from the
					// beginning of the file
					seekPtr = offset;
					break;
				case SEEK_CUR :
					// file's seek pointer is set to its current value plus the
					// offset
					seekPtr += offset;
					break;
				// file's seek pointer is set to the size of the file plus the
				// offset
				case SEEK_END :
					seekPtr = EOF + offset;
					break;
				default :
					Kernel.report("Seek error: Whence " + whence
							+ " is unrecognized");
					// return ERROR;
			}
			// clamp seek pointer to the size of the file
			// if seek pointer is negative, clamp to 0
			if (seekPtr < 0)
			{
				seekPtr = 0;
			}
			// if seek pointer is greater than file size,
			// clamp to end of file
			else if (seekPtr > EOF)
			{
				seekPtr = EOF;
			}

			// set entry's seek pointer
			fte.seekPtr = seekPtr;

			// return OK;
		}
		return seekPtr;
	}

	// ------------------------ deallocateBlocks --------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public boolean deallocateBlocks(FileTableEntry fte) 
	{
		Inode iNode;
		byte[] data;
		int block;
		// return false if fte is null
		if (fte == null)
		{
			return false;
		}
		// return false if iNode is null
		if ((iNode = fte.iNode) == null)
		{
			return false;
		}
		// return false if iNode is being used
		if (iNode.count > 1)
		{
			return false;
		}

		// deallocate direct blocks
		/*
		 * increment index by block size until iNode's length is reached, since
		 * length is a byte length. could have used iNode.direct.size and
		 * incremented by 1 through the array, but this would defeat the purpose
		 * of directSize being a private int-- so instead, we use the built-in
		 * methods.
		 */
		for (int i = 0, l = iNode.length, inc = Disk.blockSize; i < l; i += inc) 
		{
			// skip unallocated block
			if ((block = iNode.findTargetBlock(i)) == ERROR)
			{
				continue;
			}
			// deallocate block
			superblock.returnBlock(block);
			iNode.setTargetBlock(block, (short) -1);
		}

		// deallocate indirect blocks
		if ((data = iNode.deleteIndexBlock()) != null) 
		{
			for (int i = 0, l = Disk.blockSize / 2; i < l; i += 2) 
			{
				// skip unallocated block
				if ((block = SysLib.bytes2short(data, i)) == ERROR)
				{	
					continue;
				}
				// deallocate block
				superblock.returnBlock(block);
			}
		}
		// write iNode to disk
		iNode.toDisk(fte.iNumber);
		return true;
	}
}
