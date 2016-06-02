
class Test5 extends Thread {
	final static int DEFAULTFILES = 48;
	final int files;
	int fd;
	final byte[] buf16 = new byte[16];
	final byte[] buf32 = new byte[32];
	final byte[] buf24 = new byte[24];
	int size;

	public Test5(String args[]) {
		files = Integer.parseInt(args[0]);
		// SysLib.cout( "files = " + files + "\n" );
	}

	public Test5() {
		files = DEFAULTFILES;
		// SysLib.cout( "files = " + files + "\n" );
	}

	public void run() {
		double _test01 = test1() ? 2.0 : 0; // format with specified # of files
		double _test02 = test2() ? 2.0 : 0; // open "css430" with "w+"
		double _test03 = test3() ? 2.0 : 0; // write buf[16]
		double _test04 = test4() ? 2.0 : 0; // close fd
		double _test05 = test5() ? 2.0 : 0; // read buf[16] from "css430"
		double _test06 = test6() ? 1.0 : 0; // append buf[32] to "css430"
		double _test07 = test7() ? 1.0 : 0; // seek and read from "css430"
		double _test08 = test8() ? 0.5 : 0; // open "css430" with "w+"
		test9(); // open "bothell" with "w+"
		double _test10 = test10() ? 0.5 : 0; // write buf[512 * 13]
		test11(); // close fd
		double _test12 = test12() ? 0.5 : 0; // read buf[512 * 13] from
												// "bothell"
		double _test13 = test13() ? 0.5 : 0; // append buf[32] to "bothell"
		double _test14 = test14() ? 0.5 : 0; // seek and read from "bothell"
		double _test15 = test15() ? 0.5 : 0; // open "bothell" with "w+"
		double _test16 = test16() ? 0.5 : 0; // delete "css430"
		double _test17 = test17() ? 0.5 : 0; // create "uwb0" - "uwb45" of
												// buf[512 * 13]
		double _test18 = test18() ? 0.5 : 0; // "uwb1" read/written among Test5
												// and Test6
		SysLib.cout("01: Correct behavior of format......................"
				+ _test01 + "/2.0\n");
		SysLib.cout("02: Correct behavior of open........................"
				+ _test02 + "/2.0\n");
		SysLib.cout("03: Correct behavior of writing a few bytes........."
				+ _test03 + "/2.0\n");
		SysLib.cout("04: Correct behavior of close......................."
				+ _test04 + "/2.0\n");
		SysLib.cout("05: Correct behavior of reading a few bytes........."
				+ _test05 + "/2.0\n");
		SysLib.cout("06: Correct behavior of appending a few bytes......."
				+ _test06 + "/1.0\n");
		SysLib.cout("07: Correct behavior of seeking in a small file....."
				+ _test07 + "/1.0\n");
		SysLib.cout("08: Correct behavior of read/writing a small file..."
				+ _test08 + "/0.5\n");
		SysLib.cout("10: Correct behavior of writing a lot of bytes......"
				+ _test10 + "/0.5\n");
		SysLib.cout("12: Correct behavior of reading a lot of bytes......"
				+ _test12 + "/0.5\n");
		SysLib.cout("13: Correct behavior of appending to a large file..."
				+ _test13 + "/0.5\n");
		SysLib.cout("14: Correct behavior of seeking in a large file....."
				+ _test14 + "/0.5\n");
		SysLib.cout("15: Correct behavior of read/writing a large file..."
				+ _test15 + "/0.5\n");
		SysLib.cout("16: Correct behavior of delete......................"
				+ _test16 + "/0.5\n");
		SysLib.cout("17: Correct behavior of creating over 40 files ....."
				+ _test17 + "/0.5\n");
		SysLib.cout("18: Correct behavior of two fds to the same file...."
				+ _test18 + "/0.5\n");
		SysLib.cout("Total Score: "
				+ (_test01 + _test02 + _test03 + _test04 + _test05 + _test06
						+ _test07 + _test08 + _test10 + _test12 + _test13
						+ _test14 + _test15 + _test16 + _test17 + _test18)
				+ "/16.5\n");
		SysLib.exit();
	}

	private boolean test1() {
		SysLib.cout("1: format( " + files + " )...\n");
		SysLib.format(files);
		byte[] superblock = new byte[512];
		SysLib.rawread(0, superblock);
		int totalBlocks = SysLib.bytes2int(superblock, 0);
		int inodeBlocks = SysLib.bytes2int(superblock, 4);
		int freeList = SysLib.bytes2int(superblock, 8);
		int success = 0;
		SysLib.cout("1: (totalBlocks = " + totalBlocks + ") == " + 1000 + " ");
		if (totalBlocks != 1000) {
			SysLib.cout("(bad)");
		} else {
			success++;
			SysLib.cout("(good)");
		}
		SysLib.cout("\n1: (inodeBlocks = " + inodeBlocks + ") == " + files
				+ " | " + (files / 16) + " ");
		if (inodeBlocks != files && inodeBlocks != files / 16) {
			SysLib.cout("(bad)");
		} else {
			success++;
			SysLib.cout("(good)");
		}
		SysLib.cout("\n1: (freeList = " + freeList + ") == " + (1 + files / 16)
				+ " | " + (1 + files / 16 + 1) + " ");
		if (freeList != 1 + files / 16 && freeList != 1 + files / 16 + 1) {
			SysLib.cout("(bad)");
		} else {
			success++;
			SysLib.cout("(good)");
		}
		SysLib.cout("\n1: successfully completed " + success + "/" + 3 + "\n");
		return success == 3;
	}

	private boolean test2() {
		int success = 0;
		SysLib.cout("2: fd = open( \"css430\", \"w+\" )...\n");
		fd = SysLib.open("css430", "w+");
		SysLib.cout("2: (fd = " + fd + ") == 3 ");
		if (fd != 3) {
			SysLib.cout("(bad)");
		} else {
			success++;
			SysLib.cout("(good)");
		}
		SysLib.cout("\n2: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}

	private boolean test3() {
		int success = 0;
		SysLib.cout("3: size = write( fd, buf[16] )...\n");
		for (byte i = 0; i < 16; i++)
			buf16[i] = i;
		size = SysLib.write(fd, buf16);
		SysLib.cout("3: (size = " + size + ") == 16 ");
		if (size != 16) {
			SysLib.cout("(bad)");
		} else {
			success++;
			SysLib.cout("(good)");
		}
		SysLib.cout("\n3: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}

	private boolean test4() {
		int success = 0;
		SysLib.cout("4: close( fd )...");
		SysLib.close(fd);
		size = SysLib.write(fd, buf16);
		SysLib.cout("\n4: writable after closing the file? ");
		if (size > 0) {
			SysLib.cout("true (bad)");
		} else {
			SysLib.cout("false (good)");
			success++;
		}
		SysLib.cout("\n4: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}

	private boolean test5() {
		int success = 0;
		SysLib.cout("5: reopen and read from \"css430\"...\n");
		fd = SysLib.open("css430", "r");
		byte[] tmpBuf = new byte[16];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("5: (size = " + size + ") == 16 ");
		if (size != 16) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n5: tmpBuf[x] == buf16[x], x = 0..." + (size - 1) + " ");
		boolean pass = true;
		for (int i = 0; i < size; i++) {
			if (tmpBuf[i] != buf16[i]) {
				SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
						+ buf16[i] + " ");
				pass = false;
				break;
			}
		}
		if (!pass || size <= 0) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n5: successfully completed " + success + "/" + 2 + "\n");
		return success == 2;
	}

	private boolean test6() {
		int success = 0;
		SysLib.cout("6: append buf[32] to \"css430\"...\n");
		for (byte i = 0; i < 32; i++)
			buf32[i] = (byte) (i + (byte) 16);
		fd = SysLib.open("css430", "a");
		SysLib.write(fd, buf32);
		SysLib.close(fd);
		fd = SysLib.open("css430", "r");
		byte[] tmpBuf = new byte[48];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("6: (size = " + size + ") == 48 ");
		if (size != 48) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n6: tmpBuf[x] == buf16[x], x = 0...15 ");
		boolean pass = true;
		if (size > 15) {
			for (int i = 0; i < 16; i++) {
				if (tmpBuf[i] != buf16[i]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf16[i] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 16) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n6: tmpBuf[x] == buf32[x - 16], x = 16..." + (size - 1)
				+ " ");
		pass = true;
		if (size > 15) {
			for (int i = 16; i < size; i++) {
				if (tmpBuf[i] != buf32[i - 16]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf32[i - 16] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 16) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n6: successfully completed " + success + "/" + 3 + "\n");
		return success == 3;
	}

	private boolean test7() {
		int success = 0;
		SysLib.cout("7: seek and read from \"css430\"...\n");
		fd = SysLib.open("css430", "r");
		int position = SysLib.seek(fd, 10, 0);
		SysLib.cout("7: (seek(fd,10,0) = " + position + ") == 10 ");
		if (position != 10) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		byte[] tmpBuf = new byte[2];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("\n7: (seek(fd,10,0) contents = " + tmpBuf[0]
				+ ") == (byte) 10 ");
		if (tmpBuf[0] != (byte) 10) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		position = SysLib.seek(fd, 10, 0);
		position = SysLib.seek(fd, 10, 1);
		SysLib.cout("\n7: (seek(fd,10,1) = " + position + ") == 20 ");
		if (position != 20) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("\n7: (seek(fd,10,1) contents = " + tmpBuf[0]
				+ ") == (byte) 20 ");
		if (tmpBuf[0] != (byte) 20) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		position = SysLib.seek(fd, -2, 2);
		SysLib.cout("\n7: (seek(fd,-2,2) = " + position + ") == 46 ");
		if (position != 46) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("\n7: (seek(fd,-2,2) contents = " + tmpBuf[0]
				+ ") == (byte) 46 ");
		if (tmpBuf[0] != (byte) 46) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n7: successfully completed " + success + "/" + 6 + "\n");
		return success == 6;
	}

	private boolean test8() {
		int success = 0;
		SysLib.cout("8: open \"css430\" with w+...\n");
		for (short i = 0; i < 24; i++)
			buf24[i] = (byte) (24 - i);
		fd = SysLib.open("css430", "w+");
		SysLib.seek(fd, 24, 0);
		SysLib.write(fd, buf24);
		SysLib.seek(fd, 0, 0);
		byte[] tmpBuf = new byte[48];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("8: tmpBuf[x] == buf16[x], x = 0...15 ");
		boolean pass = true;
		if (size > 15) {
			for (byte i = 0; i < 16; i++)
				if (tmpBuf[i] != buf16[i]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf16[i] + " ");
					pass = false;
					break;
				}
		}
		if (!pass || size < 16) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n8: tmpBuf[x] == buf32[x - 16], x = 16...23 ");
		pass = true;
		if (size > 23) {
			for (byte i = 16; i < 24; i++)
				if (tmpBuf[i] != buf32[i - 16]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf32[i - 16] + " ");
					pass = false;
					break;
				}
		}
		if (!pass || size < 24) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n8: tmpBuf[x] == buf24[x - 24], x = 24...47 ");
		pass = true;
		if (size > 47) {
			for (byte i = 24; i < 48; i++)
				if (tmpBuf[i] != buf24[i - 24]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf24[i - 24] + " ");
					pass = false;
					break;
				}
		}
		if (!pass || size < 48) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n8: successfully completed " + success + "/" + 3 + "\n");
		return success == 3;
	}

	// final byte[] buf8192 = new byte[8192];
	final byte[] buf6656 = new byte[6656];

	private boolean test9() {
		int success = 0;
		SysLib.cout("9: fd = open( \"bothell\", \"w\" )...\n");
		fd = SysLib.open("bothell", "w+");
		SysLib.cout("9: (fd = " + fd + ") == 3 ");
		if (fd != 3) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n9: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}

	private boolean test10() {
		int success = 0;
		SysLib.cout("10: size = write( fd, buf[6656] )...\n");
		for (int i = 0; i < 6656; i++)
			buf6656[i] = (byte) (i % 256);
		size = SysLib.write(fd, buf6656);
		SysLib.cout("(size = " + size + ") == 6656 ");
		if (size != 6656) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n10: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}

	private boolean test11() {
		int success = 0;
		SysLib.cout("11: close( fd )...\n");
		SysLib.close(fd);
		size = SysLib.write(fd, buf16);
		SysLib.cout("11: writable after closing the file? ");
		if (size > 0) {
			SysLib.cout("true (bad)");
		} else {
			SysLib.cout("false (good)");
			success++;
		}
		SysLib.cout("\n11: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}

	private boolean test12() {
		int success = 0;
		SysLib.cout("12: reopen and read from \"bothell\"...\n");
		fd = SysLib.open("bothell", "r");
		byte[] tmpBuf = new byte[6656];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("(size = " + size + ") == 6656 ");
		if (size != 6656) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n12: tmpBuf[x] == buf6656[x], x = 0...6655 ");
		boolean pass = true;
		if (size > 6655) {
			for (int i = 0; i < 6656; i++) {
				if (tmpBuf[i] != buf6656[i]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf6656[i] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6656) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n12: successfully completed " + success + "/" + 2 + "\n");
		return success == 2;
	}

	private boolean test13() {
		int success = 0;
		SysLib.cout("13: append buf[32] to \"bothell\"...\n");
		fd = SysLib.open("bothell", "a");
		SysLib.write(fd, buf32);
		SysLib.close(fd);
		fd = SysLib.open("bothell", "r");
		byte[] tmpBuf = new byte[6688];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("(size = " + size + ") == 6688 ");
		if (size != 6688) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n13: tmpBuf[x] == buf6656[x], x = 0...6655 ");
		boolean pass = true;
		if (size > 6655) {
			for (int i = 0; i < 6656; i++) {
				if (tmpBuf[i] != buf6656[i]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf6656[i] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6656) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n13: tmpBuf[x] == buf32[x - 6656], x = 6656...6687 ");
		pass = true;
		if (size > 6687) {
			for (int i = 6656; i < 6688; i++) {
				if (tmpBuf[i] != buf32[i - 6656]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf32[i - 6656] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6688) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n13: successfully completed " + success + "/" + 3 + "\n");
		return success == 3;
	}

	private boolean test14() {
		int success = 0;
		SysLib.cout("14: seek and read from \"bothell\"...\n");
		fd = SysLib.open("bothell", "r");
		int position = SysLib.seek(fd, 512 * 11, 0);
		SysLib.cout("14: (seek(fd,512 * 11,0) = " + position
				+ ") == 512 * 11 == 5632 ");
		if (position != 512 * 11) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		byte[] tmpBuf = new byte[2];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("\n14: (seek(fd,512*11,0) contents = " + tmpBuf[0]
				+ ") == (byte) 5632 ");
		if (tmpBuf[0] != (byte) (512 * 11)) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		position = SysLib.seek(fd, 512 * 11, 0);
		position = SysLib.seek(fd, 512, 1);
		SysLib.cout("\n14: (seek(fd,512,1) = " + position
				+ ") == 512 * 12 == 6144 ");
		if (position != 512 * 12) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("\n14: (seek(fd,512,1) contents = " + tmpBuf[0]
				+ ") == (byte) 6144 ");
		if (tmpBuf[0] != (byte) (512 * 12)) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		position = SysLib.seek(fd, -2, 2);
		SysLib.cout("\n14: (seek(fd,-2,2) = " + position
				+ ") == 512 * 13 + 32 - 2 == 6686 ");
		if (position != 512 * 13 + 32 - 2) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("\n14: (seek(fd,-2,2) contents = " + tmpBuf[0]
				+ ") == (byte) 46 ");
		if (tmpBuf[0] != (byte) 46) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}

		SysLib.close(fd);
		SysLib.cout("\n14: successfully completed " + success + "/" + 6 + "\n");
		return success == 6;
	}

	private boolean test15() {
		int success = 0;
		SysLib.cout("15: open \"bothell\" with w+...\n");
		for (short i = 0; i < 24; i++)
			buf24[i] = (byte) (24 - i);
		fd = SysLib.open("bothell", "w+");
		SysLib.seek(fd, 512 * 12 - 3, 0);
		SysLib.write(fd, buf24);
		SysLib.seek(fd, 0, 0);
		byte[] tmpBuf = new byte[6688];
		size = SysLib.read(fd, tmpBuf);
		SysLib.cout("15: tmpBuf[x] == buf6656[x], x = 0...6140 ");
		boolean pass = true;
		if (size > 6139) {
			for (int i = 0; i < 512 * 12 - 3; i++) {
				if (tmpBuf[i] != buf6656[i]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf6656[i] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6140) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n15: tmpBuf[x] == buf24[x - 6141], x = 6141...6164 ");
		pass = true;
		if (size > 6164) {
			for (int i = 512 * 12 - 3; i < 512 * 12 - 3 + 24; i++) {
				if (tmpBuf[i] != buf24[i - (512 * 12 - 3)]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf24[i - (512 * 12 - 3)] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6165) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n15: tmpBuf[x] == buf6656[x], x = 6165...6655 ");
		pass = true;
		if (size > 6655) {
			for (int i = 512 * 12 - 3 + 24; i < 6656; i++) {
				if (tmpBuf[i] != buf6656[i]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf6656[i] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6656) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n15: tmpBuf[x] == buf32[x - 6656], x = 6656...6688 ");
		pass = true;
		if (size > 6687) {
			for (int i = 6656; i < 6688; i++) {
				if (tmpBuf[i] != buf32[i - 6656]) {
					SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == "
							+ buf32[i - 6656] + " ");
					pass = false;
					break;
				}
			}
		}
		if (!pass || size < 6688) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n15: successfully completed " + success + "/" + 4 + "\n");
		return success == 4;
	}

	private boolean test16() {
		int success = 0;
		SysLib.cout("16: delete(\"css430\")...\n");
		fd = SysLib.open("css430", "r");
		SysLib.cout("16: (fd = " + fd + ") == -1 ");
		if (fd == -1) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.delete("css430");
		fd = SysLib.open("css430", "r");
		SysLib.cout("\n16: (fd = " + fd + ") == -1 ");
		if (fd != -1) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n16: successfully completed " + success + "/" + 2 + "\n");
		return success == 2;
	}

	private boolean test17() {
		int success = 0;
		SysLib.cout("17: create uwb0-29 of 512*13...\n");
		int fdes[] = new int[29];
		boolean pass = true;
		SysLib.cout("17: fdes[x] = SysLib.open(\"uwb\" + x) > -1, x = 0...28 ");
		int lastOpen = 0;
		for (int i = 0; i < 29; i++) {
			Integer suffix = new Integer(i);
			String file = "uwb" + suffix.toString();
			fdes[i] = SysLib.open(file, "w+");
			if (fdes[i] == -1) {
				pass = false;
				SysLib.cout("SysLib.open(\"uwb" + file + "\") open failed");
				break;
			}
			lastOpen = i;
		}
		if (!pass) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		pass = true;
		SysLib.cout("\n17: SysLib.write(fdes[x], buf6656) == 6656, x = 0..."
				+ lastOpen + " ");
		for (int i = 0; i < lastOpen; i++) {
			if (SysLib.write(fdes[i], buf6656) != 6656) {
				pass = false;
				SysLib.cout("SysLib.write(fdes[" + i
						+ "], buf6656) write failed");
				break;
			}
		}
		if (!pass) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		pass = true;
		SysLib.cout("\n17: SysLib.close(fdes[x]) > -1, x = 0..." + lastOpen
				+ " ");
		for (int i = 0; i < lastOpen; i++) {
			if (SysLib.close(fdes[i]) == -1) {
				pass = false;
				SysLib.cout("SysLib.close(fdes[" + i + "]) close failed");
				break;
			}
		}
		if (!pass) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.cout("\n17: successfully completed " + success + "/" + 3 + "\n");
		return success == 3;
	}

	private boolean test18() {
		int success = 0;
		SysLib.cout("18: uwb0 read b/w Test5 & Test6...\n");
		fd = SysLib.open("uwb0", "r");
		String[] cmd = new String[2];
		cmd[0] = "Test6";
		cmd[1] = String.format("%d", fd);
		SysLib.exec(cmd);
		SysLib.join();
		SysLib.close(fd);
		SysLib.cout("18: Test6.java terminated\n");
		fd = SysLib.open("uwb1", "r");
		byte[] tmpBuf = new byte[512];
		SysLib.read(fd, tmpBuf);
		boolean pass = true;
		SysLib.cout("18: tempBuf[x] == (byte) 100, x = 0...511 ");
		for (int i = 0; i < 512; i++)
			if (tmpBuf[i] != (byte) 100) {
				pass = false;
				SysLib.cout("(tmpBuf[" + i + "] = " + tmpBuf[i] + ") == 100 ");
				break;
			}
		if (!pass) {
			SysLib.cout("(bad)");
		} else {
			SysLib.cout("(good)");
			success++;
		}
		SysLib.close(fd);
		SysLib.cout("\n18: successfully completed " + success + "/" + 1 + "\n");
		return success == 1;
	}
}