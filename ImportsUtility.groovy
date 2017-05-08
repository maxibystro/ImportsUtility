class ImportsUtility {

	static final String UMBRELLA_HEADERS_OPTION = '-uh'
	static final String PATH_SEPARATOR = ':'

	static final int INVALID_INDEX = -1
	static final String INCLUDE_STR = '#include'
	static final String IMPORT_STR = '#import'
	static final String HEADER_EXTENSION = '.h'

	static void main(String[] args) {
		File umbrellaFile = new File("/Users/Max/Documents/My_projects/Utilities/ImportsUtility/JRE.h")
		File headerFile = new File("/Users/Max/Documents/My_projects/Utilities/ImportsUtility/IOSArray.h")
		Map<String, String> frameworksHeaders = new HashMap<>()
		frameworksHeaders.putAll(readUmbrellaHeader(umbrellaFile))
		replaceLocalImportsByModular(headerFile, frameworksHeaders)
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

	static void replaceLocalImportsByModular(File file, Map<String, String> frameworksHeaders) {
		List<String> newLines = new ArrayList<>()
		file.eachLine { line ->
			String newLine = replaceImportsInString(line, frameworksHeaders)
			newLines.add(newLine)
		}

		File file2 = new File("/Users/Max/Documents/My_projects/Utilities/ImportsUtility/IOSArray2.h")
		PrintWriter writer = new PrintWriter(file2)
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