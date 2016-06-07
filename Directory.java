// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

public class Directory 
{
    private int fsize[];        // each element stores a different file size.
	private char fnames[][];    // each element stores a different file name.
    private int dirSize;  	    // size of directory
    private static int maxFileNameSize = 35; 

	// ---------------------------- Constructor ---------------------------- //
	//  Parameter:
	//   - Max number of files
	// --------------------------------------------------------------------- //
    public Directory(int totalFiles)
    { 
        fsize = new int[totalFiles];  
		
		// set file sizes to 0 to start with
        for ( int i = 0; i < totalFiles; i++ )
        {
			fsize[i] = 0;                 
		}
		
        dirSize = totalFiles;
        fnames = new char[totalFiles][maxFileNameSize];
		
        String root = "/";         
        fsize[0] = root.length();        
        root.getChars( 0, fsize[0], fnames[0], 0 ); 
    }

	// ---------------------------- bytes2directory ------------------------ //
	//  
	// --------------------------------------------------------------------- //
    public void bytes2directory(byte data[])
    {
        // assumes data[] received directory information from disk
        int offset = 0;
        for (int i = 0; i < dirSize; i++)
        {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }
        // initializes the Directory instance with this data[]
        for (int i = 0; i < dirSize; i++)
        {
            String temp = new String(data, offset, 60);
            temp.getChars(0, fsize[i], fnames[i], 0);
            offset += 60;
        }
    }

	// -------------------------- directory2bytes -------------------------- //
	//
	// --------------------------------------------------------------------- //
    public byte[] directory2bytes()
    {
        byte [] dir = new byte[64 * dirSize];
        int offset = 0;
        // converts and return Directory information into a plain byte array
        for (int i = 0; i < dirSize; i++)
        {
            SysLib.int2bytes(fsize[i], dir, offset);
            offset += 4;
        }
        for (int i = 0; i < dirSize; i++)
        {
            String temp = new String(fnames[i], 0, fsize[i]);
            byte [] bytes = temp.getBytes();
            System.arraycopy(bytes, 0, dir, offset, bytes.length);
            offset += 60;
        }
        return dir;
    }
	
	// ---------------------------- ialloc --------------------------------- //
	//
	// --------------------------------------------------------------------- //
    public short ialloc(String filename)
    {
        // filename is the one of a file to be created.
        for (short i = 0; i < dirSize; i++)
        {
            if (fsize[i] == 0)
            {
                // allocates a new inode number for this filename
                int file = filename.length() > maxFileNameSize ? maxFileNameSize : filename.length();
                fsize[i] = file;
                filename.getChars(0, fsize[i], fnames[i], 0);
                return i;
            }
        }
        return -1;
    }

	// ---------------------------- ifree ---------------------------------- //
	//
	// --------------------------------------------------------------------- //
    public boolean ifree(short iNumber) 
	{
        if(iNumber < maxFileNameSize && fsize[iNumber] > 0)
		{      //If number is valid
            fsize[iNumber] = 0;                            //Mark to be deleted
            return true;                                 //File was found
        } 
		else 
		{
            return false;                                 //File not found
        }
    }

	// ---------------------------- namei ---------------------------------- //
	//
	// --------------------------------------------------------------------- //
    public short namei(String filename)
    {
        for (short i = 0; i < dirSize; i++)
		{
            if (filename.length() == fsize[i])
			{
                String temp = new String(fnames[i], 0, fsize[i]);
                if(filename.equals(temp))
				{
                    return i;
                }
            }
        }
        return -1;
    }

	// ---------------------------- printDir ------------------------------- //
	//
	// --------------------------------------------------------------------- //
    private void printDir()
	{
        for (int i = 0; i < dirSize; i++)
		{
            SysLib.cout(i + ":  " + fsize[i] + " bytes - ");
            for (int j = 0; j < maxFileNameSize; j++)
			{
                SysLib.cout(fnames[i][j] + " ");
            }
            SysLib.cout("\n");
        }
    }
}