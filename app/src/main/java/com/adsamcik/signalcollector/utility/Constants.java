package com.adsamcik.signalcollector.utility;

public class Constants {
	//Base units to make sure there are no typos
	/**
	 * 10^3
	 */
	private static final int U_DECIMAL = 1000;

	/**
	 * (10^3) / 4 => 250
	 */
	private static final int U_QUARTER_DECIMAL = U_DECIMAL / 4;

	/**
	 * 2^10
	 */
	private static final int U_BINARY = 1024;

	/**
	 * 2^10 / 4 => 2^8
	 */
	private static final int U_QUARTER_BINARY = U_BINARY / 4;

	//Units
	//use prefix U_
	public static final int U_KILOBYTE = U_DECIMAL;
	public static final int U_KIBIBYTE = U_BINARY;

	public static final int U_MEGABYTE = U_KILOBYTE * U_DECIMAL;
	public static final int U_MEBIBYTE = U_KIBIBYTE * U_BINARY;

	//File sizes
	public static final int MIN_BACKGROUND_UPLOAD_FILE_SIZE = U_KIBIBYTE * (U_QUARTER_BINARY / 2) * 5;
	public static final int MIN_USER_UPLOAD_FILE_SIZE = U_KIBIBYTE * U_QUARTER_BINARY;
	public static final int MAX_DATA_FILE_SIZE = U_MEBIBYTE;
}
