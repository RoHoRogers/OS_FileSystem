import java.util.Vector;

// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

public class Inode 
{
	// public static Vector<Inode> iNodeList; // maintains all iNode on memory
	private final static int iNodeSize = 32; // fix to 32 bytes
	private final static int directSize = 11; // # direct pointers

	// flag states
	public final static short UNUSED = 0;
	public final static short USED = 1;
	public final static short READ = 2;
	public final static short WRITE = 3;
	public final static short DELETE = 4;
	// return values
	public final static short OK = 0;
	public final static short ERROR = -1;

	public int length; // file size in bytes
	public short count; // # file-table entries pointing to this
	public short flag; // 0 = unused, 1 = used, 2 = read, 3 = write, 4 = delete
	public short[] direct = new short[directSize]; // direct pointers
	public short indirect; // a indirect pointer

	/*
	 * Inode Default Constructor Initialize all global variables
	 */
	public Inode() 
    {
		length = 0;
		count = 0;
		flag = UNUSED;
		for (int i = 0; i < directSize; i++) {
			direct[i] = ERROR;
		}
		indirect = ERROR;		
	}

	/*
	 * Inode Constructor
	 */
	public Inode(short iNumber) {
		// read the corresponding disk block
		int block, offset;
		byte[] data;

		if (iNumber < 0)
			return;

		block = getOffset(iNumber);
		offset = (iNumber % 16) * iNodeSize;
		data = new byte[Disk.blockSize];

		SysLib.rawread(block, data);

		// locates the corresponding iNode information in that block
		length = SysLib.bytes2int(data, offset);
		offset += 4;
		count = SysLib.bytes2short(data, offset);
		offset += 2;
		flag = SysLib.bytes2short(data, offset);
		offset += 2;

		// initialize a new iNode with this information
		for (int i = 0; i < directSize; i++, offset += 2) {
			direct[i] = SysLib.bytes2short(data, offset);
		}
		indirect = SysLib.bytes2short(data, offset);
		// iNodeList.add(this);
	}

	/*
	 * toDisk() save to disk as the i-th inode
	 */
	public void toDisk(short iNumber) {
		// initialize the Inode to be added back to Disk
		int offset, block;
		byte[] data;
		if (iNumber < 0)
			return;
		block = getOffset(iNumber);
		// get the Disk block at the iNumber-th place
		offset = (iNumber % 16) * iNodeSize;
		data = new byte[Disk.blockSize];
		// same flow as constructor
		SysLib.int2bytes(length, data, offset);
		offset += 4;
		SysLib.short2bytes(count, data, offset);
		offset += 2;
		SysLib.short2bytes(flag, data, offset);
		offset += 2;

		for (int i = 0; i < directSize; i++, offset += 2) {
			SysLib.short2bytes(direct[i], data, offset);
		}

		SysLib.short2bytes(indirect, data, offset);
		offset += 2;

		// write back to Disk
		SysLib.rawwrite(block, data);
	}

	/*
	 * getOffset() return the current offset of the block
	 */
	public int getOffset(int iNumber) {
		return 1 + iNumber / 16;
	}

	/*
	 * getIndexBlockNumber() return the Index Block that's registered to this
	 * iNode
	 */
	public short getIndexBlockNumber() {
		return indirect;
	}

	/*
	 * setIndexBlock() create an Index Block to be used by indirect block
	 */
	public boolean setIndexBlock(short iNumber) {
		byte[] data;
		if (iNumber < 0)
			return false;
		// check if all 12 pointers are null
		if (indirect != ERROR)
			return false;

		for (int i = 0; i < directSize; i++)
			if (direct[i] == ERROR)
				return false;
		// set indirect pointer to the first block index
		indirect = iNumber;

		data = new byte[Disk.blockSize];
		// set all indirect blocks to -1
		for (int i = 0, l = Disk.blockSize / 2; i < l; i += 2)
			SysLib.short2bytes(ERROR, data, i);
		SysLib.rawwrite(iNumber, data);
		return true;
	}

	/*
	 * findTargetBlock() find and return the location of the block
	 */
	public short findTargetBlock(int offset) {
		int block, iBlock; // offset block, indirect offset
		byte[] data;
		block = offset / Disk.blockSize;

		// target block is in one of the direct blocks
		// return the block
		if (block < directSize)
			return direct[block];
		// this block is not registered
		if (indirect == ERROR)
			return ERROR;

		data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);

		// get the target block in indirect block and return
		iBlock = block - directSize;
		return SysLib.bytes2short(data, iBlock * 2);
	}

	/*
	 * setTargetBlock() register the block into one of the direct or indirect
	 * blocks
	 */
	public int setTargetBlock(int iNumber, short fBlock) 
    {
		int block, iBlock;
		byte[] data;
		if (iNumber < 0)
			return ERROR;

		block = iNumber / Disk.blockSize;

		// to set into one of the direct blocks
		if (block < directSize) 
        {
			// existed block
			if (direct[block] != ERROR)
				return ERROR;
			// before is unused
			if ((block > 0) && (direct[block - 1] == ERROR))
				return ERROR;
			// else register the target block
			direct[block] = fBlock;
			return OK;
		}
		// indirect blocks are unused
		if (indirect == ERROR)
			return ERROR;

		data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);
		// iBlock = block - iNodeSize;
		iBlock = block - directSize;
		if (SysLib.bytes2short(data, iBlock * 2) > 0) 
        {
			SysLib.cerr("indexBlock, indirectNo = " + block + " contents = "
					+ SysLib.bytes2short(data, iBlock * 2) + "\n");
			return ERROR;
		}

		// register the block into one of the indirect blocks
		SysLib.short2bytes(fBlock, data, iBlock * 2);
		SysLib.rawwrite(indirect, data);
		return OK;
	}

	/*
	 * deleteIndexBlock() delete one of the registered block in indirect blocks
	 */
	public byte[] deleteIndexBlock() 
    {
		byte[] data;

		// nothing to delete
		if (indirect == ERROR)
			return null;

		data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);

		// indicate deletion
		indirect = ERROR;
		return data;
	}
}