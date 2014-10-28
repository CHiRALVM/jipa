package org.chiralvm.jipa;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;

import org.chiralvm.libraries.NPAFormat.EncryptionKey;
import org.chiralvm.libraries.NPAFormat.NPAEntry;
import org.chiralvm.libraries.NPAFormat.NPAFile;
import org.chiralvm.libraries.NPAFormat.NPALibrary;

public class Main {
	public static void main(String[] args) throws UnsupportedEncodingException {
		
		out("jipa 0.1.0 by spycrab0\nNipa rewritten in Java with some extra features.\n");
		out("Using NPALibrary v"+NPALibrary.VERSION+" (API: v"+NPALibrary.API_VERSION+")\n\n");
		out("This program is based on docs and source code provided by Moogy (http://tsukuru.info/nipa/)\nThis program is licensed under the GNU General Public License v3.\n\n");
		
		if (args.length != 0) {
			if (args[0].equals("-h")) {
				if (args.length != 1) out("Invalid argument count\n");
				showHelp();
				System.exit(1);
			}
			
			if (args[0].equals("-c")) {
				if (args.length != 3) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				createNPA(args[1],args[2],false);
				return;
			}
			
			if (args[0].equals("-cz")) {
				if (args.length != 3) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				createNPA(args[1],args[2],true);
				return;
			}
			
			if (args[0].equals("-x")) {
				if (args.length != 2) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				extractNPA(args[1],null);
				return;
			}
			
			if (args[0].equals("-l")) {
				if (args.length != 2) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				listNPAEntrys(args[1],null);
				return;
			}
			
			if (args[0].equals("-i")) {
				if (args.length != 2) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				readHeaderInformation(args[1],false);
				return;
			}
			
			if (args[0].equals("-ia")) {
				if (args.length != 2) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				readHeaderInformation(args[1],true);
				return;
			}
			
			if (args[0].equals("-lg")) {
				if (args.length != 2 && args.length != 3) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				listNPAEntrys(args[1],(args.length == 3 ? args[2] : "ChaosHead"));
				return;
			}
			
			if (args[0].equals("-xg")) {
				if (args.length != 2 && args.length != 3) {
					out("Invalid argument count.\nSee -h for further help.\n");
					System.exit(1);
				}
				extractNPA(args[1],(args.length == 3 ? args[2] : "ChaosHead"));
				return;
			}
			
			System.out.println("Unknown argument.\nSee -h for further help.\n");
			showHelp();
			System.exit(1);
		} else {
			showHelp();
		}
		
		
	}
	
	public static void showHelp() {
		out("Usage:\n\nGeneral\n-h - Displays this information.\n\nInformation\n-i (file) - Show header information\n-ia (file) -Show ALL header information\n-l (file) - List contents.\n-lg [file] (id) - List contents of an encrypted archive. Default id is ChaosHead.\n\nExtraction\n-x (file) - Extract NPA archive\n-xg (file) [id] - Extract encrypted archive. Default id is ChaosHead.\n\nCreation\n-c (dir) (file) - Turn the contents of a folder into an archive\n-cz (dir) (file) - Turn the contents of a folder into an compressed archive.\n\nExamples:\n-x nss.npa - Extracts nss.npa into the folder \"nss\"\n-xg nss.npa MuramasaTr\n\nList of available Encryptions:\n\nGame - ID\n\n");
		for (Entry<String,String> entry : EncryptionKey.getIDDescriptions().entrySet()) {
			out(entry.getKey()+" - "+entry.getValue()+"\n");
		}
	}
	
	private static void listNPAEntrys(String filename,String encryption) {
		NPAFile file;

		if (encryption == null) { file = new NPAFile(new File(filename)); } else { file = new NPAFile(new File(filename),EncryptionKey.valueOf(encryption)); }
		
		file.setExtensiveMode(false);
		file.setSilentMode(false);
		
		if (!file.readHeader()) {
			out("Error: Couldn't read npa header\nExiting.\n");
			System.exit(1);
		} else {
			out("Successfully read NPA header\n");
	    }
		if(!file.readAllEntries()) {
			out("Error: Couldn't read fileheaders.\nExiting.\n");
			System.exit(1);
		} else {
			out("Successfully read "+file.getLoadedEntries().size()+" fileheaders.\n");
		}
		
		int dirs=0,files=0;
		
		for (NPAEntry entry : file.getLoadedEntries()) {
			if (entry.isFile()) {
				out(entry.getName()+" "+entry.getSize()/1024+" KB\n");
				files++;
			} else {
				out(entry.getName()+" <DIR>\n");
				dirs++;
			}
		}
		if (dirs != 1) { out("\n"+files+" file(s) and "+dirs+" directorie(s).\n"); } else { out("\n"+files+" files and "+dirs+" directory.\n"); }
		out("Done.\n");
	}
	
	private static void readHeaderInformation(String filename,boolean all) {
		NPAFile file;

		file = new NPAFile(new File(filename));
		
		file.setExtensiveMode(false);
		file.setSilentMode(false);
		
		if (!file.readHeader()) {
			out("Error: Couldn't read npa header\nExiting.\n");
			System.exit(1);
		} 
		out("Successfully read NPA header.\n\n");
		
		out("Compressed: "+(file.isCompressed() ? "Yes" : "No")+"\n");
		out("Encrypted: "+(file.isEncrypted() ? "Yes" : "No")+"\n");
		out(file.getFoldercount()+" folder(s)\n");
		out(file.getFilecount()+" file(s)\n\n");
		if (file.getTotalcount() == 1) { out(file.getTotalcount()+" entry in total.\n"); } else { out(file.getTotalcount()+" entries in total.\n"); }
		
		if (all) {
			out("\nAdditional Information:\n\n");
			out("Key 1: "+file.getKey1()+"\nKey 2: "+file.getKey2()+"\nStart position: 0x"+Integer.toHexString(file.getStartPosition())+"\n\n");
		}
		
		out("Done.\n");
	}
	
	private static void extractNPA(String filename,String encryption) {
		NPAFile file;

		if (encryption == null) { file = new NPAFile(new File(filename)); } else { file = new NPAFile(new File(filename),EncryptionKey.valueOf(encryption)); }
		
		file.setExtensiveMode(false);
		file.setSilentMode(false);
		
		if (!file.readHeader()) {
			out("Error: Couldn't read npa header\nExiting.\n");
			System.exit(1);
		} else {
			out("Successfully read NPA header.\n");
		}
		if(!file.readAllEntries()) {
			out("Error: Couldn't read file header\nExiting.\n");
			System.exit(1);
		} else {
			out("Successfully read "+file.getLoadedEntries().size()+" fileheaders.\n");
		}
		
		file.setSilentMode(true);
		file.setExtensiveMode(false);
		
		if (filename.contains(".")) {
			String[] elements = filename.split("\\.");
			filename = "";
			for (int i = 0;i < elements.length;i++) {
				if (i+1 == elements.length) {
					filename = filename.substring(0, filename.length()-1);
				} else {
					filename += elements[i]+".";
				}
			}
		} else {
			filename = "_"+filename;
		}
		
		if (!file.extractAll(filename)) {
			out("An error occured while extracting files.\nExiting.\n");
			System.exit(1);
		} else {
			return;
		}
	}
	
	private static void createNPA(String src,String filename,boolean compressed) {
		try {
			if (!new File(filename).createNewFile()) {
				System.out.println("Error: Can't create file "+filename+"!");
				System.exit(1);
			}
		} catch (IOException e) {
			System.out.println("Error: Can't create file "+filename+"!");
			System.exit(1);
		}
		if (!new File(src).exists()) {
			System.out.println("Error: Source directory doesn't exist!");
			System.exit(1);
		}
		
		if (!new File(src).isDirectory()) {
			System.out.println("Error: Source directory is a file!");
			System.exit(1);
		}
		
		if (!new File(src).exists()) {
			System.out.println("Error: Can't read source directory!");
			System.exit(1);
		}
		
		NPAFile file = new NPAFile(new File(filename));
		
		if (file.create(src, compressed)) {
			System.out.println("Done.");
		} else {
			System.out.println("Error: Couldn't create archive "+filename);
			System.exit(1);
		}
	}
	
	private static void out(String str) {
		System.out.print(str);
	}
}
