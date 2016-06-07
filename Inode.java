// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

import java.util.Vector;

public class Inode 
{
	private final static int iNodeSize = 32;       // fix to 32 bytes
	public final static int directSize = 11; 	   // # direct pointers
	private final static int nodesPerBlock = 16;
    
	public final static short UNUSED = 0;
	public final static short USED = 1;
	public final static short READ = 2;
	public final static short WRITE = 3;
	public final static short DELETE = 4;
	
	
	public int length;  						   // file size in bytes
	public short count;			 // # file-table entries pointing to this
	public short flag; 						 	   // 0 = unused, 1 = used
	public short[] direct = new short[directSize]; // direct pointers
	public short indirect; 						   // a indirect pointer

	// ------------------------ Constructor -------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public Inode() 
	{
		length = 0;
		count = 0;
		flag = 1;
		for (int i = 0; i < directSize; i++) 
		{
			direct[i] = -1;
		}
		indirect = -1;
	}


	// ------------------------ Constructor -------------------------------- //
	// 
	// --------------------------------------------------------------------- //
	public Inode(short iNumber) 
	{
		// get the number of blocks to read & allocate the bytes
		int blockLocation = 1 + iNumber / nodesPerBlock;
		byte[] rawData = new byte[Disk.blockSize];
		SysLib.rawread(blockLocation, rawData);

		// determine offset
		int offset = (iNumber % nodesPerBlock) * iNodeSize;
		
		length = SysLib.bytes2int(rawData, offset);
		offset += 4;
		count = SysLib.bytes2short(rawData, offset);
		offset += 2;
		flag = SysLib.bytes2short(rawData, offset);
		offset += 2;

		// Complete the construction of the iNode
		for (int i = 0; i < directSize; i++) 
		{
			direct[i] = SysLib.bytes2short(rawData, offset);
			offset += 2;
		}
		
		indirect = SysLib.bytes2short(rawData, offset);
		offset +=2;    
	}


	// ------------------------ toDisk ------------------------------------- //
	// save to disk as the i-th inode
	// --------------------------------------------------------------------- //
	public int toDisk(short iNumber) 
	{
		// initialize the Inode to be added back to Disk
		int blockLocation = 1 + iNumber / nodesPerBlock;
		int offset = 0;
		byte[] rawData = new byte[iNodeSize];

		SysLib.int2bytes(length, rawData, offset);
		offset += 4;
		SysLib.short2bytes(count, rawData, offset);
		offset += 2;
		SysLib.short2bytes(flag, rawData, offset);
		offset += 2;

		for (int i = 0; i < directSize; i++) 
		{
			SysLib.short2bytes(direct[i], rawData, offset);
			offset += 2;
		}

		SysLib.short2bytes(indirect, rawData, offset);
		offset += 2;
		
		
		byte[] newData = new byte[Disk.blockSize];
		SysLib.rawread(blockLocation, newData);
		
		offset = (iNumber % nodesPerBlock) * iNodeSize;

		// write back to Disk 
		System.arraycopy(rawData, 0, newData, offset, iNodeSize);
		SysLib.rawwrite(blockLocation, newData);
		return 0;
	}

	// ------------------------ freeIndirect ------------------------------- //
	// free an indirect block
	// --------------------------------------------------------------------- //
	public byte[] freeBlock() 
	{
		if (indirect == -1)
		{
			return null;   // nO deletion necessary
		}
		else
		{
			byte[] rawData = new byte[Disk.blockSize];
			SysLib.rawread(indirect, rawData);
			indirect = -1;
			return rawData;
		}
	}


	// ------------------------ setBlock ----------------------------------- //
	// register the block into one of the direct or indirect blocks
	// --------------------------------------------------------------------- //
	boolean setBlock(short blockLocation)
    {
        for (int i = 0; i < directSize; i++)
        {
            if (direct[i] == -1)
			{
                return false;
            }
        }

        if (indirect != -1)
		{
            return false;
        }

        indirect = blockLocation;
        byte[] data = new byte[Disk.blockSize];

        for(int i = 0; i < (Disk.blockSize / 2); i++)
		{
            SysLib.short2bytes((short) -1, data, i * 2);
        }
        SysLib.rawwrite(blockLocation, data);

        return true;
    }

	
	// ------------------------ findBlock ---------------------------------- //
	// Return the location of a block
	// --------------------------------------------------------------------- //
	public int findBlock(int offset) 
	{
		int target = offset / Disk.blockSize;
		int blockLocation; 
		byte[] rawData = new byte[Disk.blockSize];

		// target block is in one of the direct blocks
		// return the block
		if (target < directSize)
		{
			return direct[target];
		}

		if (indirect < 0)
		{
			return -1;     // not an available block
		}
		
		SysLib.rawread(indirect, rawData);

		// get the target block in indirect block and return
		blockLocation = (target - directSize) * 2; 
		return SysLib.bytes2short(rawData, blockLocation);
	}
	
    // ------------------------ getBlock ----------------------------------- //
	// register the block into one of the direct or indirect blocks
	// --------------------------------------------------------------------- //
	public int getBlock(int entry, short offset) 
	{
	        int target = entry/ Disk.blockSize;

        if (target < directSize){
            if(direct[target] >= 0)
			{
                return -1;
            }

            if ((target > 0 ) && (direct[target - 1 ] == -1))
			{
                return -2;
            }

            direct[target] = offset;
            return 0;
        }

        if (indirect < 0)
		{
            return -3;
        }

        else
		{
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect,data);

            int blockSpace = (target - directSize) * 2;
            if ( SysLib.bytes2short(data, blockSpace) > 0)
			{
                return -1;
            }
            else
            {
                SysLib.short2bytes(offset, data, blockSpace);
                SysLib.rawwrite(indirect, data);
            }
        }
        return 0;
	}
}