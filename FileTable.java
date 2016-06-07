// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

import java.util.Vector;

public class FileTable 
{
    public final static int UNUSED = 0;
    public final static int USED = 1;
    public final static int READ = 2;
    public final static int WRITE = 3;

    private Vector<FileTableEntry> table;    // the actual entity of this file table
    private Directory dir;        			 // the root directory


	// ------------------------ Constructor -------------------------------- //
	// Builds the beginnings of the FileTableEntry table
	// --------------------------------------------------------------------- //
    public FileTable(Directory directory) 
	{ 
        table = new Vector<FileTableEntry>();     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

	// ------------------------ falloc ------------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public synchronized FileTableEntry falloc(String fileName, String mode) 
	{
		Inode inode = null; 
 	    short iNumber = -1;   

        while (true) 
		{
            // get the inumber form the inode for given file name
            //iNumber = (fileName.equals("/") ? (short) 0 : dir.namei(fileName));
			if (fileName.equals("/"))
			{
				iNumber = (short) 0;			
			}
			else
			{
				iNumber = dir.namei(fileName);	
			}

            
            if (iNumber >= 0)    // if iNode exists
			{
                inode = new Inode(iNumber);  

                if (mode.equals("r"))
				{
                    if (inode.flag == READ || inode.flag == USED || inode.flag == UNUSED) 
					{
                        inode.flag = READ;   // safe to read
                        break;                       
                    } 
					else if (inode.flag == WRITE) // currently writing
					{
                        try {
                            wait();
                        } catch (InterruptedException e) { }
                    }
                } 
				else 
				{
                    if (inode.flag == USED || inode.flag == UNUSED) 
					{
                        inode.flag = WRITE;
                        break;
                    } 
					else 
					{
                        try {
                            wait();
                        } catch (InterruptedException e) { }
                    }
                }
            } 
			else if (!mode.equals("r"))   // create new iNode
			{
                iNumber = dir.ialloc(fileName);
                inode = new Inode(iNumber);
                inode.flag = WRITE;
                break;
            } 
			else 
			{
                return null;
            }
        }

		// increase count
        inode.count++;  
        inode.toDisk(iNumber);
        
		// make new FTE
        FileTableEntry fte = new FileTableEntry(inode, iNumber, mode);
        table.addElement(fte);
        return fte;
	}
	
	// ------------------------ ffree -------------------------------------- //
	//  Locate and free an FTE
	// --------------------------------------------------------------------- //
	public synchronized boolean ffree(FileTableEntry fte) 
	{
		Inode inode = new Inode(fte.iNumber);

        if (table.remove(fte))
        {
            if (inode.flag == READ)
            {
                if (inode.count == 1)
                {
                    // free this file table entry.
                    notify();
                    inode.flag = USED;
                }
            }
            else if (inode.flag == WRITE)
            {
                inode.flag = USED;
                notifyAll();
            }
			
            inode.count--;  // lower count
            inode.toDisk(fte.iNumber); 
			
            return true;
        }
        return false;
	}

	// ------------------------ fempty ------------------------------------- //
	//  Return true if empty
	// --------------------------------------------------------------------- //
	public synchronized boolean fempty() 
	{
		return table.isEmpty(); 
	}
}