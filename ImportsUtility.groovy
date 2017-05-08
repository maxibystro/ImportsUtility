class ImportsUtility {

	static final int EXIT_STATUS_INPUT_ERROR = 1

	static final String UMBRELLA_HEADERS_OPTION = '-uh'
	static final String PATH_SEPARATOR = ':'

	static final int INVALID_INDEX = -1
	static final String INCLUDE_STR = '#include'
	static final String IMPORT_STR = '#import'
	static final String HEADER_EXTENSION = '.h'

	static void main(String[] args) {
		List<String> umbrellaHeaderPaths = null
		List<String> sourceHeaderPaths = new ArrayList()
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (UMBRELLA_HEADERS_OPTION.equals(arg)) {
				if (i + 1 < args.length) {
					umbrellaHeaderPaths = args[++i].split(PATH_SEPARATOR)
				}
			} else {
				sourceHeaderPaths.add(arg)
			}
		}
		if (umbrellaHeaderPaths == null || umbrellaHeaderPaths.size() == 0) {
			println 'Umbrella header paths are not set. Use -uh option.'
    		System.exit(EXIT_STATUS_INPUT_ERROR)
		}
		if (sourceHeaderPaths.size() == 0) {
			println 'Source header paths are not set.'
    		System.exit(EXIT_STATUS_INPUT_ERROR)
		}

		Map<String, String> headerFrameworkMap = new HashMap<>()
		for (String umbrellaHeaderPath : umbrellaHeaderPaths) {
			File file = new File(umbrellaHeaderPath)
			if (!file.exists() || file.isDirectory()) {
				println "Can not open ${umbrellaHeaderPath}."
	    		System.exit(EXIT_STATUS_INPUT_ERROR)
			}
			headerFrameworkMap.putAll(readUmbrellaHeader(file))
		}
		for (String sourceHeaderPath : sourceHeaderPaths) {
			File file = new File(sourceHeaderPath)
			if (!file.exists() || file.isDirectory()) {
				println "Can not open ${sourceHeaderPath}."
	    		System.exit(EXIT_STATUS_INPUT_ERROR)
			}
			replaceLocalWithFrameworkImports(file, headerFrameworkMap)
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

	static void replaceLocalWithFrameworkImports(File file, Map<String, String> frameworksHeaders) {
		List<String> newLines = new ArrayList<>()
		file.eachLine { line ->
			String newLine = replaceImportsInString(line, frameworksHeaders)
			newLines.add(newLine)
		}
		PrintWriter writer = new PrintWriter(file)
   		newLines.each { line -> writer.println(line) }
   		writer.close()
	}

	static String replaceImportsInString(String str, Map<String, String> frameworksHeaders) {
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
		String frameworkName = frameworksHeaders.get(headerPath)
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