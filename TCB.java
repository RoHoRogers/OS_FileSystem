// ------------------------------------------------------------------------ //
//  Josh Trygg & Kevin Rogers 
//  CSS 430
//  Final Project - Unix "like" file system
// ------------------------------------------------------------------------ //

public class TCB 
{
	private Thread thread = null;
	private int tid = 0;
	private int pid = 0;
	private boolean terminated = false;
	private int sleepTime = 0;

	// User file descriptor table
	// each entry pointing to a file (structure) table entry
	public FileTableEntry[] ftEnt = null;

	public TCB(Thread newThread, int myTid, int parentTid) 
    {
		thread = newThread;
		tid = myTid;
		pid = parentTid;
		terminated = false;

		ftEnt = new FileTableEntry[32];

		// initialize array
        // fd[0], fd[1], fd[2] are null for std input, std output, std err
		for (int i = 0; i < 32; i++)
			ftEnt[i] = null;
		

		Kernel.report("a new thread (thread=" + thread + " tid=" + tid
				+ " pid=" + pid + ")");
	}

	public synchronized Thread getThread() 
    {
		return thread;
	}

	public synchronized int getTid() 
    {
		return tid;
	}

	public synchronized int getPid() 
    {
		return pid;
	}

	public synchronized boolean setTerminated() 
    {
		return (terminated = true);
	}

	public synchronized boolean getTerminated() 
    {
		return terminated;
	}

	// added for the file system
	public synchronized int getFd(FileTableEntry entry) 
    {
		if (entry == null)
			return -1;
		for (int i = 3; i < 32; i++) 
        {
			if (ftEnt[i] == null) 
            {
				ftEnt[i] = entry;
				return i;
			}
		}
		return -1;
	}

	public synchronized FileTableEntry returnFd(int fd) 
    {
		// check for requested entry
		if (fd < 3 || fd >= 32)
			return null;
		// if found, return the FTE and set pointer to null
		FileTableEntry oldEnt = ftEnt[fd];
		ftEnt[fd] = null;
		return oldEnt;
	}

	public synchronized FileTableEntry getFte(int fd) 
    {
		// get the FTE
		return fd >= 3 && fd < 32 ? ftEnt[fd] : null;
	}
}