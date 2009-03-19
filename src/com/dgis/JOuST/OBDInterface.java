
package com.dgis.JOuST;

import com.dgis.util.Logger;
import com.dgis.util.PrettySimpleConfig;

/*
 * Copyright (C) 2009 Giacomo Ferrari
 * This file is part of JOuST.
 *  JOuST is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JOuST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JOuST.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author Giacomo Ferrari
 * Copyright (C) 2009 Giacomo Ferrari
 */

public class OBDInterface {
	private static final String CONFIG_FILENAME="JOuST.cfg";
	public static final String MAJOR_VERSION="0";
	public static final String MINOR_VERSION="01";
	public static final String VERSION = MAJOR_VERSION+"."+MINOR_VERSION;
	public static final String APPLICATION_NAME="JOuST "+VERSION;
	
	protected static final PrettySimpleConfig CONFIG;
	static{
		Logger.getInstance().setLevel(Logger.LEVEL_VERBOSE);
		Logger.getInstance().setPrintStream(System.err);
		CONFIG = new PrettySimpleConfig(CONFIG_FILENAME);
	}
}
