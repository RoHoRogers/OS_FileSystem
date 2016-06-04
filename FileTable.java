// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

import java.util.Vector;

public class FileTable 
{
	private Vector<FileTableEntry> table; // the actual entity of this file
											// table
	private Directory dir; // the root directory

	// ------------------------ Constructor -------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public FileTable(Directory directory) 
	{ // constructor
		table = new Vector<FileTableEntry>(); // instantiate a file (structure)
												// table
		dir = directory; // reference to the directory from the file system
	}

	// ------------------------ falloc ------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public synchronized FileTableEntry falloc(String fname, String m) 
	{
		short mode = FileTableEntry.getMode(m);
		short iNumber = -1;
		Inode iNode = null;
		FileTableEntry fte;
		// if mode is invalid, return null
		if (mode == -1)
			return null;
		while (true) 
		{
			// allocate / retrieve and register the corresponding inode using
			// dir
			iNumber = fname.equals("/") ? 0 : dir.namei(fname);
			
			if (iNumber < 0) 
			{ // file does not exist
				if (mode == FileTableEntry.READONLY) // do not allocate file if
														// read only
					return null;
				// allocate Inode with default constructors
				if ((iNumber = dir.ialloc(fname)) < 0)
					return null; // or not
				iNode = new Inode();
				break;
			}
			
			iNode = new Inode(iNumber);
			if (iNode.flag == Inode.DELETE)
				return null; // no more to open
			if (iNode.flag == Inode.UNUSED || iNode.flag == Inode.USED)
				break; // no need to wait for anything
			// flags left include read and write
			if (mode == FileTableEntry.READONLY && // mode is "r"
					iNode.flag == Inode.READ)
				break; // no need to wait on READ
			
			// if the flag is WRITE for "r", or READ or WRITE for "w" "w+" or
			// "a" we wait in all cases
			try 
			{
				wait();
			} 
			catch (InterruptedException e) {}
		}
		// increment this iNode's count
		iNode.count++;

		// immediately write back this iNode to the disk
		iNode.toDisk(iNumber);

		// allocate new file table entry for this file name
		fte = new FileTableEntry(iNode, iNumber, m);

		// add FTE to table
		table.add(fte);

		// return a reference to this FTE
		return fte;
	}
	
	// ------------------------ ffree -------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public synchronized boolean ffree(FileTableEntry fte) 
	{
		if (fte == null)
		{
				return true; // if null, it's already free
		}

		Inode iNode = fte.iNode;
		short iNumber = fte.iNumber;

		// the FTE was not found in my table
		if (!table.removeElement(fte))
		{
				return false;
		}

		// decrement this iNode's count
		if (iNode.count > 0)
		{
				iNode.count--;
		}

		// when no more FTEs point to iNode, flag = 0
		if (iNode.count == 0)
		{
			iNode.flag = 0;
		}

		// save the corresponding iNode to the disk
		iNode.toDisk(iNumber);

		// notify waiting threads
		if (iNode.flag == Inode.READ || iNode.flag == Inode.WRITE)
		{
				notify();
		}

		// free this FTE
		fte = null; // the FTE is now eligible for garbage collection

		// the FTE was found in my table
		return true;
	}

	// ------------------------ fempty ------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public synchronized boolean fempty() 
	{
		return table.isEmpty(); // return if table is empty
	}
}
