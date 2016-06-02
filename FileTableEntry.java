// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

// POtential custom changes....
// -- get rid of getMode, there doesn't seem to be much use for it
// -- along with the "enums"
//

public class FileTableEntry 
{
	public int seekPtr;            // a file seek pointer
	public final Inode iNode;      // a reference to its iNode
	public final short iNumber;    // this iNode number
	public int count;              // # threads sharing this entry
	public final int mode;         // "r", "w", "w+", or "a"

	// modes
	public static final short READONLY = 0; // read only
	public static final short WRITEONLY = 1; // write only
	public static final short READWRITE = 2; // read and write
	public static final short APPEND = 3; // append

	public FileTableEntry(Inode i, short inumber, String m) 
    {
		seekPtr = 0; // the seek pointer is set to the file top
		iNode = i;
		iNumber = inumber;
		count = 1; // at least on thread is using this entry
		mode = getMode(m); // once access mode is set, it never changes
		iNode.count++; // update iNode count
		if (mode == APPEND) // if mode is append
			seekPtr = iNode.length; // seekPtr points to the end of file
	}

	public static short getMode(String mode) 
    {
		mode = mode.toLowerCase();
		if (mode.compareTo("r") == 0)
			return READONLY;
		if (mode.compareTo("w") == 0)
			return WRITEONLY;
		if (mode.compareTo("w+") == 0)
			return READWRITE;
		if (mode.compareTo("a") == 0)
			return APPEND;
		return -1;
	}
}