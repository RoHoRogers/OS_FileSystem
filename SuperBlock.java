// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

public class SuperBlock 
{
	private final static int defaultInodeBlocks = 64;
	public int totalBlocks; // default 1000
	public int totalInodes; // default 64 (or 4 blocks including Inodes)
	public int freeList; // default 5 (block#0 = super, blocks#1,2,3,4 = inodes)


	// ------------------------ Constructor -------------------------------- //
	//  Default 
	// --------------------------------------------------------------------- //
	public SuperBlock() 
	{
		this(defaultInodeBlocks);
	}


	// ------------------------ Constructor -------------------------------- //
	// Parameter:
	//   - Total Disk Size
	// --------------------------------------------------------------------- //
	public SuperBlock(int sizeOfDisk) 
	{
		// allocate superblock
		byte[] block = new byte[Disk.blockSize];
		// read superblock from disk
		SysLib.rawread(0, block);
		totalBlocks = SysLib.bytes2int(block, 0);
		totalInodes = SysLib.bytes2int(block, 4);
		freeList = SysLib.bytes2int(block, 8);

		// success case
		if (freeList >= 2 && totalInodes > 0 && totalBlocks == sizeOfDisk) 
		{
			return;
		} 
		else 
		{
			totalBlocks = sizeOfDisk;
			format(defaultInodeBlocks);
		}
	}


	// ------------------------ format ------------------------------------- //
	// Redo the formatting of Inodes and Superblock by the given format
	// number (iNodes). For example, 32 will yield 2 blocks of Inodes.
	// --------------------------------------------------------------------- //
	public void format(int iNodes) 
	{
		if (iNodes < 0)
		{
			iNodes = defaultInodeBlocks;
		}

		totalInodes = iNodes;
		//inodeBlocks = totalInodes;
		Inode dummy = null;

		for (int i = 0; i < totalInodes; i++)
		{
			dummy = new Inode();
			dummy.flag = 0;
			dummy.toDisk((short) i);
		}

		freeList = (totalInodes / 16) + 2;

		byte [] newEmpty = null;    // new dummy block

		for (int i = freeList; i < totalBlocks - 1; i++)
		{
			newEmpty = new byte [Disk.blockSize];

			//erase
			for (int j = 0; j < Disk.blockSize; j++)
			{
				newEmpty[j] = 0;
			}

			SysLib.int2bytes(i+1, newEmpty, 0);
			SysLib.rawwrite(i, newEmpty);
		}

		newEmpty = new byte[Disk.blockSize];

		//erase
		for (int j = 0; j < Disk.blockSize; j++)
		{
			newEmpty[j] = 0;
		}

		SysLib.int2bytes(-1, newEmpty, 0);
		SysLib.rawwrite(totalBlocks - 1, newEmpty);
		byte[] newBlock = new byte[Disk.blockSize];

		// copy back all components
		SysLib.int2bytes(totalBlocks, newBlock, 0);
		SysLib.int2bytes(totalInodes, newBlock, 4);
		SysLib.int2bytes(freeList, newBlock, 8);

		// write new super
		SysLib.rawwrite(0, newBlock);

    }
	


	// ------------------------ sync --------------------------------------- //
	// Write back totalBlocks, totalInodes, and freeList to Disk in order
	// to update the new specs in of the Superblock
	// --------------------------------------------------------------------- //
	public void sync() 
	{
		byte[] blockData = new byte[Disk.blockSize];
		SysLib.int2bytes(freeList, blockData, 8);
		SysLib.int2bytes(totalBlocks, blockData, 0);
		SysLib.int2bytes(totalInodes, blockData, 4);
		SysLib.rawwrite(0, blockData);
		Kernel.report("Superblock synchronized");
	}


	// ------------------------ getFreeBlock ------------------------------- //
	// Dequeue the top block from the free list
	// --------------------------------------------------------------------- //
	public int getFreeBlock() 
	{
		if (freeList < 0 || freeList > totalBlocks)
		{
			return -1;   // block error
		}
		else
		{
			int block = freeList;
			byte[] blockData = new byte[Disk.blockSize];
			SysLib.rawread(freeList, blockData);

			freeList = SysLib.bytes2int(blockData, 0);
			return block;   // return locale of free block
		}
	}


	// ------------------------ returnBlock -------------------------------- //
	// Enqueue a given block to the end of the free list Return
	// true if the operation is successful
	// --------------------------------------------------------------------- //
	public boolean returnBlock(int blockNumber) 
	{
		if (blockNumber < 0 || blockNumber > totalBlocks)
		{
			int temp = 0;
			int freeBlock = freeList;

			byte [] next = new byte[Disk.blockSize];

			byte [] newBlock = new byte[Disk.blockSize];


			//erase block
			for(int i = 0; i < Disk.blockSize; i++)
			{
				newBlock[i] = 0;
			}

			SysLib.int2bytes(-1, newBlock, 0);

			while (freeBlock != -1)
			{
				SysLib.rawread(freeBlock, next);

				temp = SysLib.bytes2int(next, 0);

				if (temp == -1)
				{
					
					SysLib.int2bytes(blockNumber, next, 0);
					SysLib.rawwrite(freeBlock, next);
					SysLib.rawwrite(blockNumber, newBlock);

					return true;   
				}

				freeBlock = temp;
			}
		}
	    // Invalid block 
		return false;   
	}
}


