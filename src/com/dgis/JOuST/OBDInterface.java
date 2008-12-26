package com.dgis.JOuST;

import com.dgis.util.PrettySimpleConfig;

public class OBDInterface {
	private static final String CONFIG_FILENAME="JOuST.cfg";
	public static final int MAJOR_VERSION=0;
	public static final String MINOR_VERSION="01a";
	public static final String VERSION = MAJOR_VERSION+"."+MINOR_VERSION;
	public static final String APPLICATION_NAME="JOuST "+VERSION;
	
	protected static final PrettySimpleConfig CONFIG;
	static{
		CONFIG = new PrettySimpleConfig(CONFIG_FILENAME);
	}
}
