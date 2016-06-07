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

	public final int SEEK_SET = 0;
	public final int SEEK_CUR = 1;
	public final int SEEK_END = 2;

	// ------------------------ Constructor -------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public FileSystem(int blocks) 
	{

		superblock = new SuperBlock(blocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);

		FileTableEntry directoryEnt = open("/", "r");
		int size = fsize(directoryEnt);
		
		if (size > 0)  // Does the directory have data?
		{
			byte[] dirData = new byte[size];     
			read(directoryEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(directoryEnt);
	}

	// ------------------------ sync --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public void sync() 
	{
		// write the "/" file from disk
		FileTableEntry root = open("/", "w");
		// get directory data in bytes
		byte[] dirData = directory.directory2bytes();
		// write the file table entry
		write(root, dirData);
		close(root);
		// tell super block to write to the disk
		superblock.sync();
	}

	// ------------------------ format ------------------------------------- //
	// The parameter "files" specifies the number of files to be created
	// (the number of inodes to be allocated) in the file system.  
	// --------------------------------------------------------------------- //
	public boolean format(int files) 
	{
		superblock.format(files);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		return true;
	}

	// ------------------------ open --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public FileTableEntry open(String filename, String mode) 
	{
		FileTableEntry fte = filetable.falloc(filename, mode);
		if (mode == "w")
    	{
    		// if so, make sure all blocks are unallocated
    		if ( !deallocAllBlocks( fte ))
    		{
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
		synchronized(fte) 
		{
			// decrease the number of users
			fte.count--;

			if (fte.count == 0) 
			{
				return filetable.ffree(fte);
			}
			return true;
		}
	}

	// ------------------------ fsize -------------------------------------- //
	// Return size in bytes
	// --------------------------------------------------------------------- //
	public int fsize(FileTableEntry fte) 
	{
		  //cast the entry as synchronized
    	synchronized(fte)
    	{
	        // Set a new Inode object to the entries Inode
			Inode inode = fte.inode;
	        // return the length on the new Inode object
    		return inode.length;
    	}
	}

	// ------------------------ read --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public int read(FileTableEntry fte, byte[] buffer) 
	{
    	//entry is index of file in process open-file table
    	//this accesses system wide open file table
    	//data blocks accessed, file control block returned
    	
        //check write or append status
		if ((fte.mode == "w") || (fte.mode == "a"))
		{
			return -1;
		}

        int size  = buffer.length;   //set total size of data to read
        int readBuf = 0;            //track data read
        int rError = -1;            //track error on read
        int blockSize = 512;        //set block size
        int bytes = 0;            //track how much is left to read

        //cast the fte as synchronized
        //loop to read chunks of data
        
        synchronized(fte)
        {
        	while (fte.seekPtr < fsize(fte) && (size > 0))
        	{
        		int currentBlock = fte.inode.findBlock(fte.seekPtr);
        		if (currentBlock == rError)
        		{
        			break;
        		}
				byte[] data = new byte[blockSize];
        		SysLib.rawread(currentBlock, data);
        		
        		int dataOffset = fte.seekPtr % blockSize;
        		int blocksLeft = blockSize - bytes;
        		int fileLeft = fsize(fte) - fte.seekPtr;
        		
        		if (blocksLeft < fileLeft)
				{
					bytes = blocksLeft;
				}
				else
				{
					bytes = fileLeft;
				}

				if (bytes > size)
				{
					bytes = size;
				}

        		System.arraycopy(data, dataOffset, buffer, readBuf, bytes);
        		readBuf += bytes;
        		fte.seekPtr += bytes;
        		size -= bytes;
        	}
        	return readBuf;
        }
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
		int bytesWritten = 0;
		int bufferSize = buffer.length;
		int blockSize = 512;

		if (fte == null || fte.mode == "r")
		{
			return -1;
		}

		synchronized (fte)
		{
			while (bufferSize > 0)
			{
				int location = fte.inode.findBlock(fte.seekPtr);
				if (location == -1)  // if block unblemished
				{
					short newLocation = (short) superblock.getFreeBlock();
					int testPtr = fte.inode.getBlock(fte.seekPtr, newLocation);

					if (testPtr == -3)
					{
						short freeBlock = (short) this.superblock.getFreeBlock();

						// indirect pointer is empty
						if (!fte.inode.setBlock(freeBlock))
						{
							return -1;
						}

						// check block pointer error
						if (fte.inode.getBlock(fte.seekPtr, newLocation) != 0)
						{
							return -1;
						}

					}
					else if (testPtr == -2 || testPtr == -1)
					{
						return -1;
					}

					location = newLocation;
				}

				byte [] tempBuff = new byte[blockSize];
				SysLib.rawread(location, tempBuff);

				int tempPtr = fte.seekPtr % blockSize;
				int diff = blockSize - tempPtr;

				if (diff > bufferSize)
				{
					System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, bufferSize);
					SysLib.rawwrite(location, tempBuff);

					fte.seekPtr += bufferSize;
					bytesWritten += bufferSize;
					bufferSize = 0;
				}
				else 
				{
					System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, diff);
					SysLib.rawwrite(location, tempBuff);

					fte.seekPtr += diff;
					bytesWritten += diff;
					bufferSize -= diff;
				}
			}

			// update inode length if seekPtr larger

			if (fte.seekPtr > fte.inode.length)
			{
				fte.inode.length = fte.seekPtr;
			}
			fte.inode.toDisk(fte.iNumber);
			return bytesWritten;
		}
	}
	
	
	// ------------------------ delete ------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public boolean delete(String file) 
	{
		// pick TCB
		FileTableEntry tcb = open(file, "w");       
		if (directory.ifree(tcb.iNumber) && close(tcb)) 
		{
			// Deletion
			return true;     
		} 
		else 
		{
			return false;     
		}
	}

	// ------------------------ seek --------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public int seek(FileTableEntry fte, int offset, int whence) 
	{
		synchronized (fte)
		{
			switch(whence)
			{
				//beginning of file
				case SEEK_SET:
					//set seek pointer to offset of beginning of file
					fte.seekPtr = offset;
					break;
				// current position
				case SEEK_CUR:
					fte.seekPtr += offset;
					break;
				// if from end of file
				case SEEK_END:
					// set seek pointer to size + offset
					fte.seekPtr = fte.inode.length + offset;
					break;
				// unsuccessful
				default:
					return -1;
			}

			if (fte.seekPtr < 0)
			{
				fte.seekPtr = 0;
			}

			if (fte.seekPtr > fte.inode.length)
			{
				fte.seekPtr = fte.inode.length;
			}

			return fte.seekPtr;
		}
	}

	// ------------------------ deallocAllBlocks --------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public boolean deallocAllBlocks(FileTableEntry fte) 
	{
        short invalid = -1;
    	if (fte.inode.count != 1)
		{
			SysLib.cerr("Null Pointer");
			return false;
		}

		for (short blockId = 0; blockId < fte.inode.directSize; blockId++)
		{
			if (fte.inode.direct[blockId] != invalid)
			{
				superblock.returnBlock(blockId);
				fte.inode.direct[blockId] = invalid;
			}
		}

		byte [] data = fte.inode.freeBlock();

		if (data != null)
		{
			short blockId;
			while((blockId = SysLib.bytes2short(data, 0)) != invalid)
			{
				superblock.returnBlock(blockId);
			}
		}
		fte.inode.toDisk(fte.iNumber);
		return true;
	}
}