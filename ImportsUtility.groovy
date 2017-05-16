class ImportsUtility {

	static final int EXIT_STATUS_INPUT_ERROR = 1

	static final String OPTION_PREFIX = '-'
	static final String DEFAULT_FRAMEWORK_NAME_OPTION = 'dfn'
	static final String UMBRELLA_HEADERS_OPTION = 'uh'
	static final String PATH_SEPARATOR = ':'
	static final String FIX_UMBRELLA_HEADERS_OPTION = 'fuh'

	static final int INVALID_INDEX = -1
	static final String INCLUDE_STR = '#include'
	static final String IMPORT_STR = '#import'
	static final String HEADER_EXTENSION = '.h'
	static final String UMBRELLA_HEADER_NAME_SUFFIX = 'Umbrella'

	static void main(String[] args) {
		String defaultFramework = null
		List<String> umbrellaHeaderPaths = null
		List<String> sourcePaths = new ArrayList()
		boolean fixUmbrellaHeaders = false
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith(OPTION_PREFIX)) {
				String optionName = arg.substring(1, arg.length())
				boolean unsupportedOption = false
				if (FIX_UMBRELLA_HEADERS_OPTION.equals(optionName)) {
					fixUmbrellaHeaders = true
				} else if (i + 1 < args.length) {
					String nextArg = args[++i]
					if (DEFAULT_FRAMEWORK_NAME_OPTION.equals(optionName)) {
						defaultFramework = nextArg
					} else if (UMBRELLA_HEADERS_OPTION.equals(optionName)) {
						umbrellaHeaderPaths = nextArg.split(PATH_SEPARATOR)
					} else {
						unsupportedOption = true
					}
				} else {
					unsupportedOption = true
				}
				if (unsupportedOption) {
					println "Unsupported option ${optionName}."
    				System.exit(EXIT_STATUS_INPUT_ERROR)
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
				if (fixUmbrellaHeaders) {
					fixUmbrellaHeader(file)
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
				List<File> normalFiles = collectNormalFiles(file, true)
				for (File normalFile : normalFiles) {
					replaceLocalWithFrameworkImports(normalFile, headerFrameworkMap, defaultFramework)
				}
			} else {
				replaceLocalWithFrameworkImports(file, headerFrameworkMap, defaultFramework)
			}
		}
	}

	static List<File> collectNormalFiles(File dirFile, boolean recursively) {
		List<File> normalFiles = new ArrayList<>()
		for (File file : dirFile.listFiles()) {
			if (!file.isDirectory()) {
				normalFiles.add(file)	
			} else if (recursively) {
				normalFiles.addAll(collectNormalFiles(file, recursively))
			}
		}
		return normalFiles
	}

	static Map<String, String> readUmbrellaHeader(File file) {
		Map<String, String> headers = new HashMap<>()
		String frameworkName = getFrameworkNameByUmbrellaHeaderFile(file)
		String headerPathPrefix = '<' + frameworkName + '/'
		int index, toIndex
		file.eachLine { line ->
			index = endIndexOfImport(line)
			if (index == INVALID_INDEX) return
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

	static String getFrameworkNameByUmbrellaHeaderFile(File file) {
		String fileNameWithExt = file.getName()
		String fileName = fileNameWithExt.take(fileNameWithExt.lastIndexOf('.'))
		return fileName.endsWith(UMBRELLA_HEADER_NAME_SUFFIX) ? fileName.substring(0, fileName.length() - UMBRELLA_HEADER_NAME_SUFFIX.length()) : fileName
	}

	static void rewriteFileWithLineConverter(File file, Closure cl) {
		List<String> newLines = new ArrayList<>()
		file.eachLine { line ->
			String newLine = cl(line)
			newLines.add(newLine)
		}
		PrintWriter writer = new PrintWriter(file)
   		newLines.each { line -> writer.println(line) }
   		writer.close()
	}

	static void fixUmbrellaHeader(File file) {
		String frameworkName = getFrameworkNameByUmbrellaHeaderFile(file)
		Closure cl = { String line ->
			return fixImportInString(line, frameworkName)
		}
		rewriteFileWithLineConverter(file, cl)
	}

	static String fixImportInString(String str, String frameworkName) {
		int index = endIndexOfImport(str)
		if (index == INVALID_INDEX) return str
		index = endIndexOf(str, '<', index)
		if (index == INVALID_INDEX) return str
		if (str.regionMatches(false, index, frameworkName, 0, frameworkName.length())) return str
		return str.substring(0, index) + frameworkName + '/' + str.substring(index, str.length())
	}

	static void replaceLocalWithFrameworkImports(File file, Map<String, String> frameworksHeaders, String defaultFramework) {
		Closure cl = { String line ->
			return replaceLocalWithFrameworkImportInString(line, frameworksHeaders, defaultFramework)
		}
		rewriteFileWithLineConverter(file, cl)
	}

	static String replaceLocalWithFrameworkImportInString(String str, Map<String, String> frameworksHeaders, String defaultFramework) {
		int index = endIndexOfImport(str)
		if (index == INVALID_INDEX) return str
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

	static int endIndexOfImport(String line) {
		int index = indexOfNotWhitespace(line, 0)
		if (line.regionMatches(false, index, INCLUDE_STR, 0, INCLUDE_STR.length())) {
			index += INCLUDE_STR.length()
		} else if (line.regionMatches(false, index, IMPORT_STR, 0, IMPORT_STR.length())) {
			index += IMPORT_STR.length()
		} else {
			return INVALID_INDEX
		}
	}

}