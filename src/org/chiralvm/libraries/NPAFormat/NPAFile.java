package org.chiralvm.libraries.NPAFormat;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class NPAFile {
	private File file;
	private EncryptionManager enc;
	private CReader hr,fr;
	private boolean extensive=false,silent=true;
	private ArrayList<NPAEntry> loadedEntries = new ArrayList<NPAEntry>();
	private EncryptionKey encryptionKey;
	private int k1,k2,compress,encrypt,totalCount,folderCount,fileCount,_null,start; //Header information

	public NPAFile(File file,EncryptionKey encryptionKey) {
		this.file=file;
		this.encryptionKey=encryptionKey;
	}
	public NPAFile(File file) { this.file=file; }
	
	//Core properties
	public ArrayList<NPAEntry> getLoadedEntries() { return loadedEntries; }
	public EncryptionKey getEncryptionKey() { return encryptionKey; }
	public File getFile() { return file; }
	
	//Header properties
	public int getKey1() { return k1; }
	public int getKey2() { return k2; }
	public int getFoldercount() { return folderCount; }
	public int getFilecount() { return fileCount; }
	public int getTotalcount() { return totalCount; }
	public int getStartPosition() { return start; }
	public boolean isEncrypted() { return (encrypt == 1); }
	public boolean isCompressed() { return (compress == 1); }
	
	//Modes
	public void setSilentMode(boolean silent) { this.silent=silent; }
	public void setExtensiveMode(boolean extensive) { this.extensive=extensive; }
	public boolean silentMode() { return silent; }
	public boolean extensiveMode() { return extensive; }
	
	/**
	 * Extracts single file
	 * 
	 * @param entry entry to be extracted
	 * @return dst destination path (e.g. 'a/abc.txt')
	 */
	public boolean extractFile(NPAEntry entry,String dst) {
		try {
	    fr = new CReader(new FileInputStream(file)); //Init Reader
		OutputStream os = new FileOutputStream(new File(dst)); //Init Writer
		
		fr.skip(entry.getOffset()+start+0x29); //Jumping to file start
		
		int size = entry.getCSize();
		byte[] buf = new byte[size];
		
		if (encrypt == 1) {
			
			int key = enc.fCrypt(entry);
			int len = 0x1000;
			int[] buffer = fr.readUnsignedBytes(size);
			
			if (encryptionKey.equals(EncryptionKey.Django)) {
				len += entry.getName().length();
			}
			
			for (int i = 0;i < buffer.length && i < len;i++) {
				
				int ptr = buffer[i];
				
				buffer[i] = ((encryptionKey.getKey()[ptr]-key)-i); //Decrypting the byte
				 
				int value=buffer[i];
				
				while (value < 0) { value += 256; } //Making the byte unsigned for dummies
				
				buffer[i] = value;
			}
									
			for (int i = 0;i < buffer.length;i++) {
				buf[i] = (byte) buffer[i];
			}
			
		} else {
			int[] buffer = fr.readUnsignedBytes(size);
			System.out.println("Buffer data for "+entry.getName());
			for (int i = 0;i < buffer.length;i++) {
				System.out.print(Integer.toHexString(buf[i])+" ");
				buf[i] = (byte) buffer[i];
			}
			System.out.print("\n");
		}
		
		if (compress == 1) {
			Inflater inf = new Inflater();
			inf.setInput(buf);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(buf.length);  
			byte[] uc_buf = new byte[1024]; //Compression buffer
			
			while (!inf.finished()) {
				try {
					int c = inf.inflate(uc_buf);
					out.write(uc_buf,0,c);
				} catch (DataFormatException e) {
					System.out.println("Error while uncompressing!");
					e.printStackTrace();
				}
			}
			
			byte[] uncompressed = out.toByteArray();
			os.write(uncompressed); //Writing uncompressed data
			
			out.close();
			inf.end();
		} else {
			os.write(buf); //Writing data
		}
		
		os.flush(); //Flushing writer
		os.close(); //Closing writer
		fr.close(); //Closing reader
		
		} catch (IOException io) {
			if (!silent) System.out.println("Error while reading "+entry.getName()+"!");
			if (!silent && extensive) io.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Searches for entries with a certain type
	 * 
	 * @param type the type you are looking for (e.g. 1 = directory, 2 = file)
	 * @return list of entries which are of the desired type
	 */
	public ArrayList<NPAEntry> getEntryByType(int type) {
		ArrayList<NPAEntry> entryList = new ArrayList<>();
		for (NPAEntry entry : loadedEntries) {
			if (entry.getType() == type) {
				entryList.add(entry);
			}
		}
		
		return entryList;
	}
	
	/**
	 * Extracts an single entry from the NPA file
	 * 
	 * @param entry The entry you want to extract
	 * @param dst destination directory
	 * @return exit code
	 */
	public boolean extractEntry(NPAEntry entry,String dst) {
		if (encryptionKey == null && encrypt == 1) {
			System.out.println("Encrypted archive;No Key given");
			return false;
		}
		
		if (encryptionKey != null && encrypt == 0) {
			System.out.println("Non-encrypted archive;Encryption key given");
		}
		
		if (new File(dst).exists()) {
				if (!new File(dst).canWrite()) {
					System.out.println("Can't write file!");
					return false;
				}
				if (!new File(dst).isDirectory()) {
					System.out.println("Path is no directory!");
					return false;
				}
		}
		
		String path = (dst.endsWith(File.separator)) ? dst : dst+File.separator;
		
			if (entry.isFile()) {
				extractFile(entry,path+entry.getName());
			} else if (entry.isDirectory()) {
				if (!new File(path+entry.getName()).mkdirs() && (!new File(path+entry.getName()).exists() && !new File(path+entry.getName()).isDirectory())) {
					if (!silent) System.out.println("Failed to create directory '"+path+entry.getName()+"'");
					return false;
				}
			} else {
				if (!silent) System.out.println(entry.getName()+": unknown entry type.");
			}
			if (!silent && extensive) System.out.println(path+entry.getName());

		System.out.println("Done.");
		return true;
	}
	
	/**
	 * Extracts all entries from the NPA file
	 * 
	 * @param dst destination directory
	 * @return exit code
	 */
	public boolean extractAll(String dst) {	
		if (!silent) System.out.print("Extracting...\n\n");
		
		int files=0,dirs=0;
		for (NPAEntry entry : loadedEntries) {
			if (entry.isFile()) files++;
			if (entry.isDirectory()) dirs++;
			
			boolean before = silent;
			this.setSilentMode(true);
			if (!extractEntry(entry,dst)) {
				return false;
			}
			this.setSilentMode(before);
			
			if (!silent && extensive) { System.out.println(entry.getName()+" "+(entry.isFile() ? new BigDecimal(((double)(files+dirs)/(double)loadedEntries.size())*100).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue()+"%" : "DIR")); } else { System.out.print("."); }
		}
		
		if (dirs != 1) { System.out.println("\n"+files+" file(s) and "+dirs+" directories.\n"); } else { System.out.println("\n"+files+" file(s) and "+dirs+" directory.\n"); }
		
		if (!silent && !extensive) System.out.print("\n");
		if(!silent) System.out.println("Done.");
		return true;
	}
	
	/**
	 * Get an NPAEntry by filename
	 * 
	 * @param name filename (Note: Its comparing the original names (e.g. dm55/8000510b02.ogg )
	 * @return entry with desired name
	 */
	public NPAEntry getEntryByFileName(String name) {
		for (NPAEntry entry : loadedEntries) {
			if (entry.getOriginalName().equalsIgnoreCase(name)) {
				return entry;
			}
		}
		return null;
	}
	
	/**
	 * Read NPA Header
	 * 
	 * @return exit code
	 */
	public boolean readHeader() {
		
		if (!file.exists()) {
			if (!silent) System.out.println("Error: File doesn't exist.");
			return false;
		}

		if (!file.canRead()) {
			if (!silent) System.out.println("Error: Can't read file.");
			return false;
		}
		
		
		
		try {
			hr = new CReader(new BufferedInputStream(new FileInputStream(file)));
		
			byte[] bstart = hr.readBytes(7); //Reading header
			String fstart = new String(bstart, "SJIS"); //Converting the header to string
		
			if (!silent && extensive) System.out.print("Checking 0x00->0x07...");
			
			//Checking if header is equals NPA\x01\x00\x00\x00
			if (fstart.equals("NPA"+new String(new byte[] {1,0,0,0},"SJIS"))) {
			
				if (!silent && extensive) System.out.println("valid");
			
				//Reading Header
				
				k1 = hr.readDWord(); //Reading Key1
				k2 = hr.readDWord(); //Reading Key2
				compress = hr.readWord();
				encrypt = hr.readWord();
				totalCount = hr.readDWord();
				folderCount = hr.readDWord();
				fileCount = hr.readDWord();
				_null = hr.readQWord();
				start = hr.readDWord();
						
				
				if (!silent && extensive) {
					System.out.print("\nDATA:\n\n");
					System.out.print("key1="+k1+"\nkey2="+k2+"\ncompress="+compress+"\n");
					System.out.print("encrypt="+encrypt+"\ntotalCount="+totalCount+"\nfolderCount="+folderCount+"\n");
					System.out.print("fileCount="+fileCount+"\nnull="+_null+"\nstart="+start+"\n~~~~\n\n");
				}
				return true;
			} else {
				if (!silent) System.out.println("Invalid Header.");
				return false;
			}
		} catch (IOException io) {
			if (!silent && extensive) io.printStackTrace();
			if (!silent) System.out.println("An IO Exception occured.");
			return false;
		}
	}
	
	/**
	 * Read the next NPAEntry
	 * 
	 * @return exit code
	 */
	public boolean readNextEntry(int amount) {
	    if (hr == null) {
	    	if (!silent) System.out.println("Reader is not initalized");
	    	return false;
	    }
	    
	    //If the reader position is smaller than 0x29 the header wasn't read
		if (hr.getPosition() < 0x29) { 
			if (!silent) System.out.println("Invalid reader position.");
			return false;
		}
		
		if (loadedEntries.size()+amount > totalCount) {
			if (!silent) System.out.println("Can't read that much "+loadedEntries.size()+amount+">"+totalCount);
			return false;
		}
		
		if (enc == null) enc = new EncryptionManager(encryptionKey,k1,k2); //Initialising the encryption manager
		
		int entries_before = loadedEntries.size();
		
		for (int i = entries_before;i < amount+entries_before;i++) {
			
			NPAEntry entry;
			
	        int fileCSize=0,fileSize=0,
	            fileOffset=0,fileId=0,
	            nameLength=0,fileType=0;
			
			String fileName="";
			
			long oldpos = hr.getPosition();
			
			try {
			
			//Reading the filenames length (in bytes);
			nameLength = hr.readDWord();
			//Reading the filename
			byte[] fb = hr.readBytes(nameLength); 
			
			//Decrypting the filename
			for (int x=0; x < nameLength;x++) {
				fb[x] += enc.hCrypt(x,i);
			}

			fileName = new String(fb,"SJIS"); //Converting the filename to string (Note: format is Shift_JIS)
			
			//Reading the remaining properties
			fileType = hr.readWord();
			fileId = hr.readDWord();
			fileOffset = hr.readDWord();
			fileCSize = hr.readDWord();
			fileSize = hr.readDWord();
			
			} catch(IOException io) {
				if (!silent) System.out.println("IO Exception occured while parsing file: "+i);
				return false;
			}
			
			//Turning read properties into a instance of NPAEntry
			entry = new NPAEntry(
					fileName,
					loadedEntries.size(),
					fileType,
					fileId,
					fileOffset,
					fileCSize,
					fileSize);
			
			loadedEntries.add(entry);
			
			if (entry.isFile()) { 
				if (!silent) System.out.println("[0x"+Long.toHexString(oldpos)+" - 0x"+Long.toHexString(hr.getPosition())+"] "+entry.getName()+" "+entry.getCSize()/1024+" KB "); 
				} else if (entry.isDirectory()) {  
					if (!silent) System.out.println("[0x"+Long.toHexString(oldpos)+" - 0x"+Long.toHexString(hr.getPosition())+"] "+entry.getName()+" DIR"); 
					} else {  
						if (!silent) System.out.println("[0x"+Long.toHexString(oldpos)+" - 0x"+Long.toHexString(hr.getPosition())+"] "+entry.getName()+" ?"); 
				}
			if (!silent && extensive) System.out.print("~~~~\nID: "+fileId+"\nType: "+fileType+"\nOffset: "+fileOffset+"\nSize (Comp.): "+fileCSize+" KB\nSize: "+fileSize+" KB\n\n");
		}
		
		
		return true;
	}
	
	/**
	 * Read all NPAEntries
	 * If some them are already read, it will just read the remaining ones.
	 * 
	 * @return exit code
	 */
	public boolean readAllEntries() {
	    if (hr == null) {
	    	if (!silent) System.out.println("Reader is not initalized");
	    	return false;
	    }
	    
		if (hr.getPosition() < 0x29) {
			if (!silent) System.out.println("Invalid reader position.");
			return false;
		}
		
		readNextEntry(totalCount-loadedEntries.size()); //Subtracting the entries which are already read
		return true;
	}
	
	/*
	 * NPA Creation Stuff
	 */
	
	private int id = 0,subdir= 0,offset=0;
	private ArrayList<NPAEntry> npaEntries = new ArrayList<>();
	String basedir;
		
	
	private void addEntry(String path,String name,boolean isFile,int curid,int subdir) {
		NPAEntry entry = new NPAEntry();
		String origname = new File(basedir).toURI().relativize(new File(path).toURI()).getPath().replaceAll(File.separator, "\\\\");
		
		//Directory shouldn't end with a \
		if (origname.endsWith("\\")) origname = origname.substring(0, origname.length()-1);
		
		entry.setOriginalName(origname);
		
		if (!silent && extensive) System.out.println("orig.:"+origname);
		
		entry.setID(subdir);
		if (compress == 1 && isFile) {
			entry.setCSize((int) new File(path).length());
			
			/*
			 * Well it looks like we have to predetermine how big the file will be if its getting compressed,
			 * as there is no .skip() in the DataOutputStream.
			 */
			
			try {
				CReader fsReader = new CReader(new FileInputStream(new File(path)));
				byte[] buffer = fsReader.readBytes(entry.getSize());
				fsReader.close();
			
				byte[] zbuffer = new byte[entry.getSize()];
				Deflater deflater = new Deflater();
				deflater.setInput(buffer);
				deflater.finish();
				entry.setCSize(deflater.deflate(zbuffer));
				} catch (IOException io) {
					if (!silent && extensive) io.printStackTrace();
					if (!silent) System.out.println("IOException occured :/");
				}
		} else {
			if (isFile) entry.setCSize((int) new File(path).length());
		}
		if (isFile) entry.setSize((int) new File(path).length());
		
		if (!isFile) {
			entry.setSize(0);
			entry.setCSize(0);
		}
		
		if (isFile) { entry.setType(2); } else { entry.setType(1); }	
		try {
			start += entry.getOriginalName().getBytes("SJIS").length;
		} catch (UnsupportedEncodingException e) {
			if (!silent && extensive) e.printStackTrace();
			if (!silent) System.out.println("You're f*cked! SJIS is not supported");
		}
		
		if (totalCount > 0) {
	        entry.setOffset(offset);
			if (isFile) offset += entry.getCSize();
		}
		
		totalCount++;
		npaEntries.add(entry);
	}
	
	private void parseDirectory(File directory) {
		int curid=id++;
	    for (File f : directory.listFiles()) {
	    	if (!silent && extensive) System.out.println(f.getAbsolutePath());
	    	if (!f.isHidden()) {
	    		addEntry(f.getPath(),f.getName(),f.isFile(),curid,subdir);
	    		if (f.isDirectory()) subdir++;
	    		if (f.isDirectory()) {
	    			folderCount++;
	    			parseDirectory(f);
	    			subdir--;
	    		}
	    	} else {
	    		if (!silent) System.out.println(f.getName()+" is hidden;skipping...");
	    	}
	    }
	}
	
	/**
	 * Create a NPA file
	 * 
	 * @param src Source directory
	 * @param dst Destination directory
	 * @return exit code
	 */
	public boolean create(String src,boolean comp) {
		start = 0;
		
		System.out.println("Creating archive "+file.getName()+"...");
		
		basedir = src;
		
	    //Parse directory (recrusive)
		System.out.print("Scanning directory structure\nMight take a while\n");
	    parseDirectory(new File(src));
		
		//Preparing header Values
	    k1 = 0x4147414E;
	    k2 = 0x21214F54;
	    compress = (comp ? 1 : 0);
	    encrypt = 0;
	    fileCount = totalCount-folderCount;
	    start += totalCount*0x15;
		if (enc == null) enc = new EncryptionManager(encryptionKey,k1,k2);
	    
	    //Converting Header values into bytes
	    byte[] npaHead = new byte[] {78,80,65,1,0,0,0}; //NPA\x01\x00\x00\x00 encoded.
	    byte[] npaKey1 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(k1).array();
	    byte[] npaKey2 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(k2).array();
	    byte[] npaTotalcount = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalCount).array();
	    byte[] npaFoldercount = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(folderCount).array();
	    byte[] npaFilecount = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fileCount).array();
	    byte[] npaNull = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(1852121856).array();
	    byte[] npaStart = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(start).array();
	    byte npaEncrypt = 0,npaCompress = 0;
	    
	    npaEncrypt = 0;
	    npaCompress = (byte)compress;
	    npaEncrypt = (byte)encrypt;
	    	    
	    try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
			
			System.out.println("Writing the archive header");
			
			//Writing the NPA Header
			dos.write(npaHead); //7 bytes
			dos.write(npaKey1); //4 bytes
			dos.write(npaKey2); //4 bytes
			dos.write(npaCompress); //1 byte
			dos.write(npaEncrypt); //1 byte
			dos.write(npaTotalcount); //4 bytes
			dos.write(npaFoldercount); //4 bytes
			dos.write(npaFilecount); //4 bytes
			dos.write(npaNull); //8 bytes
			dos.write(npaStart); //4 bytes
			
			System.out.println("Writing entries");
			
			//Writing the file headers
			for (int i = 0;i < npaEntries.size();i++) {
				NPAEntry entry = npaEntries.get(i);
				byte[] chName = new byte[entry.getOriginalName().getBytes("SJIS").length]; 
				for (int x = 0;x < chName.length;x++) {
					chName[x] = (byte) (entry.getOriginalName().getBytes("SJIS")[x] - enc.hCrypt(x, i));
				}
				
				byte[] entFSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chName.length).array();
				byte[] entName = ByteBuffer.allocate(chName.length).order(ByteOrder.LITTLE_ENDIAN).put(chName).array();
				byte[] entType = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte)entry.getType()).array();
				byte[] entID = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(entry.getID()).array();
				byte[] entOffset = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(entry.getOffset()).array();
				byte[] entCSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(entry.getCSize()).array();
				byte[] entSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(entry.getSize()).array();
				
				dos.write(entFSize);
				dos.write(entName);
				dos.write(entType);
				dos.write(entID);
				dos.write(entOffset);
				dos.write(entCSize);
				dos.write(entSize);
			}
			
			if (!silent) System.out.println("Writing file data");
			
			//Writing the file data
			for (NPAEntry entry : npaEntries) {
				if (entry.isFile()) {
					CReader fsReader = new CReader(new FileInputStream(new File(src+File.separator+entry.getOriginalName().replaceAll("\\\\",File.separator))));
					int[] buf = fsReader.readUnsignedBytes(entry.getCSize());
					byte[] buffer = new byte[entry.getCSize()];
					for (int i = 0;i < entry.getCSize();i++) {
						buffer[i] = (byte) buf[i]; //Just don't ask, just don't...
					}
					fsReader.close();
					
					if (compress == 1) {
						byte[] zbuffer = new byte[entry.getSize()];
						Deflater deflater = new Deflater();
						deflater.setInput(buffer);
						deflater.finish();
						int cSize = deflater.deflate(zbuffer);
						for (int j = 0;j < cSize;j++) dos.write(zbuffer[j]);
					} else {
						dos.write(buffer);
						dos.flush();
					}
					if (entry.getCSize() > 1024) {
						if (!silent) System.out.println(entry.getOriginalName()+" "+entry.getCSize()/1024+" KB");
					} else {
						if (!silent) System.out.println(entry.getOriginalName()+" "+entry.getCSize()+" B");
					}
				}
			}
			
			if (!silent) System.out.println("Done.");
			
			dos.close();
			
			return true;
		} catch (FileNotFoundException e) {
			if (!silent) System.out.println("Error while writing archive: Couldn't locate file.");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			if (!silent) System.out.println("Error while writing file: Failed to create file.");
			e.printStackTrace();
			return false;
		}
	}
}
