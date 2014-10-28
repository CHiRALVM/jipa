package org.chiralvm.jipa;

import java.io.File;

import org.chiralvm.libraries.NPAFormat.NPAFile;

public class XtractTest {
	public static void main(String[] args) {
	    NPAFile file = new NPAFile(new File("/home/max/jipaTstC.npa"));
		file.setSilentMode(false);
		
		file.create("/home/max/npaThis", true);
		
		System.exit(0);
	}
}
