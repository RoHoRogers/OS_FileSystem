// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

public class FileTableEntry 
{
    public int seekPtr;                 //    a file seek pointer
    public final Inode inode;           //    a reference to its inode
    public final short iNumber;         //    this inode number
    public int count;                   //    # threads sharing this entry
    public final String mode;           //    "r", "w", "w+", or "a"
	
	// ------------------------ Constructor -------------------------------- //
	// 
	// --------------------------------------------------------------------- //
    public FileTableEntry ( Inode i, short inumber, String m ) 
	{
        seekPtr = 0;             // the seek pointer is set to the file top
        inode = i;
        iNumber = inumber;
        count = 1;               // at least on thread is using this entry
        mode = m;                // once access mode is set, it never changes
        if ( mode.compareTo( "a" ) == 0 ) // if mode is append,
            seekPtr = inode.length;        // seekPtr points to the end of file
    }
}
