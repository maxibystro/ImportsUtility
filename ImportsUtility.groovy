class ImportsUtility {

	static final int EXIT_STATUS_INPUT_ERROR = 1

	static final String OPTION_PREFIX = '-'
	static final String DEFAULT_FRAMEWORK_NAME_OPTION = 'dfn'
	static final String UMBRELLA_HEADERS_OPTION = 'uh'
	static final String PATH_SEPARATOR = ':'

	static final int INVALID_INDEX = -1
	static final String INCLUDE_STR = '#include'
	static final String IMPORT_STR = '#import'
	static final String HEADER_EXTENSION = '.h'

	static void main(String[] args) {
		String defaultFramework = null
		List<String> umbrellaHeaderPaths = null
		List<String> sourcePaths = new ArrayList()
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith(OPTION_PREFIX)) {
				if (i + 1 < args.length) {
					String nextArg = args[++i]
					String optionName = arg.substring(1, arg.length())
					if (DEFAULT_FRAMEWORK_NAME_OPTION.equals(optionName)) {
						defaultFramework = nextArg
					} else if (UMBRELLA_HEADERS_OPTION.equals(optionName)) {
						umbrellaHeaderPaths = nextArg.split(PATH_SEPARATOR)
					}
				}
			} else {
				sourcePaths.add(arg)
			}
		}
		if (sourcePaths.size() == 0) {
			println 'Source paths are not set.'
    		System.exit(EXIT_STATUS_INPUT_ERROR)
		}

		Map<String, String> headerFrameworkMap = new HashMap<>()
		if (umbrellaHeaderPaths != null) {
			for (String umbrellaHeaderPath : umbrellaHeaderPaths) {
				File file = new File(umbrellaHeaderPath)
				if (!file.exists() || file.isDirectory()) {
					println "Can not open ${umbrellaHeaderPath}."
		    		System.exit(EXIT_STATUS_INPUT_ERROR)
				}
				headerFrameworkMap.putAll(readUmbrellaHeader(file))
			}
		}
		for (String sourcePath : sourcePaths) {
			File file = new File(sourcePath)
			if (!file.exists()) {
				println "Can not open ${sourcePath}."
	    		System.exit(EXIT_STATUS_INPUT_ERROR)
			}
			if (file.isDirectory()) {
				for (File dirFile : file.listFiles()) {
					if (!dirFile.isDirectory()) {
						replaceLocalWithFrameworkImports(dirFile, headerFrameworkMap, defaultFramework)			
					}
				}
			} else {
				replaceLocalWithFrameworkImports(file, headerFrameworkMap, defaultFramework)
			}
		}
	}

	static Map<String, String> readUmbrellaHeader(File file) {
		Map<String, String> headers = new HashMap<>()
		String fileName = file.getName()
		String frameworkName = fileName.take(fileName.lastIndexOf('.'))
		String headerPathPrefix = '<' + frameworkName + '/'
		int index, toIndex
		file.eachLine { line ->
			index = indexOfNotWhitespace(line, 0)
			if (line.regionMatches(false, index, INCLUDE_STR, 0, INCLUDE_STR.length())) {
				index += INCLUDE_STR.length()
			} else if (line.regionMatches(false, index, IMPORT_STR, 0, IMPORT_STR.length())) {
				index += IMPORT_STR.length()
			} else {
				return
			}
			index = line.indexOf(headerPathPrefix, index)
			if (index == INVALID_INDEX) {
				return
			} else {
				index += headerPathPrefix.length()
			}
			toIndex = line.indexOf('>', index)
			if (toIndex == INVALID_INDEX) return
			headers.put(line.substring(index, toIndex), frameworkName)
		}
		return headers
	}

	static void replaceLocalWithFrameworkImports(File file, Map<String, String> frameworksHeaders, String defaultFramework) {
		List<String> newLines = new ArrayList<>()
		file.eachLine { line ->
			String newLine = replaceImportsInString(line, frameworksHeaders, defaultFramework)
			newLines.add(newLine)
		}
		PrintWriter writer = new PrintWriter(file)
   		newLines.each { line -> writer.println(line) }
   		writer.close()
	}

	static String replaceImportsInString(String str, Map<String, String> frameworksHeaders, String defaultFramework) {
		int index = 0
		index = endIndexOf(str, IMPORT_STR, index)
		if (index == INVALID_INDEX) {
			index = endIndexOf(str, INCLUDE_STR, index)
			if (index == INVALID_INDEX) return str
		}
		int firstQuotesIndex = str.indexOf('\"', index)
		if (firstQuotesIndex == INVALID_INDEX) return str
		int secondQuotesIndex = str.indexOf('\"', firstQuotesIndex + 1)
		if (secondQuotesIndex == INVALID_INDEX) return str
		if (!str.regionMatches(false, secondQuotesIndex - HEADER_EXTENSION.length(), HEADER_EXTENSION, 0, HEADER_EXTENSION.length())) return str
		String headerPath = str.substring(firstQuotesIndex + 1, secondQuotesIndex)
		String knownFrameworkName = frameworksHeaders.get(headerPath)
		String frameworkName = knownFrameworkName != null ? knownFrameworkName : defaultFramework
		if (frameworkName == null) return str
		return str.substring(0, firstQuotesIndex) + '<' + frameworkName + '/' + headerPath + '>' + str.substring(secondQuotesIndex + 1)
	}

	static int indexOfNotWhitespace(String str, int fromIndex) {
		for (int i = fromIndex; i < str.length(); i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return i
			}
		}
		return INVALID_INDEX
	}

	static int endIndexOf(String str, String of, int fromIndex) {
		int index = str.indexOf(of, fromIndex)
		return index != INVALID_INDEX ? index + of.length() : INVALID_INDEX
	}

}