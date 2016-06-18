package inss;

interface IParser {
	/* type of argument */
	public static final int TYPEFALSE = 1;
	public static final int TYPETRUE = 2;
	public static final int TYPENEXTARG = 3;
	public static final int TYPETWONEXTARG = 4;
	public static final int TYPEOPTNEXTARG = 5;
	/** the second (only) is optional */
	public static final int TYPEOPTTWONEXTARG = 6;
	public static final int TYPELIST = 7;

	public static final String UNDEFINED = "undefined";

}