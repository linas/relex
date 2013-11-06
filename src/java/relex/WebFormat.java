/*
 * Copyright 2008 Novamente LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package relex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;

import relex.corpus.DocSplitter;
import relex.corpus.DocSplitterFactory;
import relex.output.CompactView;

/**
 * The WebFormat class provides the central processing point for parsing
 * sentences and extracting semantic relationships from them.  The main()
 * proceedure is usable as a stand-alone document analyzer; it supports
 * several flags modifying the displayed output.
 *
 * The primary output format generated by this class is the so-called
 * "compact file format". This format is usefule for the long-term
 * storage of parse results. This format is intended to serve as input
 * to later text processing stages -- in this way, if the later stages
 * are modified, one does not have to re-parse the original English input.
 * That is, reading in the "compact file format" is hundreds/thousands of
 * times less CPU-expensive than the original English-language parse.
 *
 * The primary interface is the processSentence() method,
 * which accepts one sentence at a time, parses it, and extracts
 * relationships from it.
 */
public class WebFormat extends RelationExtractor
{
	/**
	 * Main entry point
	 */
	public static void main(String[] args)
	{
		String callString = "WebFormat" +
			" [-h (show this help)]" +
			" [-l (do not show parse links)]" +
			" [-m (do not show parse metadata)]" +
			" [-n max number of parses to display]" +
			" [-t (do not show constituent tree)]" +
			" [--url source URL]" +
			" [--maxParseSeconds N]";
		HashSet<String> flags = new HashSet<String>();
		flags.add("-h");
		flags.add("-l");
		flags.add("-m");
		flags.add("-t");
		HashSet<String> opts = new HashSet<String>();
		opts.add("-n");
		opts.add("--maxParseSeconds");
		opts.add("--url");
		Map<String,String> commandMap = CommandLineArgParser.parse(args, opts, flags);

		String url = null;
		String sentence = null;
		int maxParses = 30;
		int maxParseSeconds = 60;

		CompactView cv = new CompactView();

		if (commandMap.get("-l") != null) cv.showLinks(false);
		if (commandMap.get("-m") != null) cv.showMetadata(false);
		if (commandMap.get("-t") != null) cv.showConstituents(false);

		// Check for optional command line arguments.
		try
		{
			maxParses = commandMap.get("-n") != null ?
				Integer.parseInt(commandMap.get("-n").toString()) : 1;

			maxParseSeconds = commandMap.get("--maxParseSeconds") != null ?
				Integer.parseInt(commandMap.get("--maxParseSeconds").toString()) : 60;
			url = commandMap.get("--url");
		}
		catch (Exception e)
		{
			System.err.println("Unrecognized parameter.");
			System.err.println(callString);
			e.printStackTrace();
			return;
		}

		if (commandMap.get("-h") != null)
		{
			System.err.println(callString);
			return;
		}

		cv.setMaxParses(maxParses);
		cv.setSourceURL(url);

		WebFormat re = new WebFormat();
		re.setAllowSkippedWords(true);
		re.setMaxParses(maxParses);
		re.setMaxParseSeconds(maxParseSeconds);

		// Pass along the version string.
		cv.setVersion(re.getVersion());

		// If sentence is not passed at command line, read from standard input:
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		DocSplitter ds = DocSplitterFactory.create();

		// Collect statistics
		int sentence_count = 0;
		ParseStats stats = new ParseStats();

		System.out.println(cv.header());

		while(true)
		{
			// Read text from stdin.
			while (sentence == null)
			{
				try {
					sentence = stdin.readLine();
					if ((sentence == null) || "END.".equals(sentence))
					{
						System.out.println(cv.footer());
						return;
					}
				} catch (IOException e) {
					System.err.println("Error reading sentence from the standard input!");
				}

				// Buffer up input text, and wait for a whole,
				// complete sentence before continuing.
				ds.addText(sentence + " ");
				sentence = ds.getNextSentence();
			}

			while (sentence != null)
			{
				Sentence sntc = re.processSentence(sentence);

				// Print output
				System.out.println (cv.toString(sntc));

				// Collect statistics
				sentence_count ++;
				stats.bin(sntc);

				if (sentence_count%20 == 0)
				{
					System.err.println ("\n" + stats.toString());
				}

				sentence = ds.getNextSentence();
			}
		}
	}
}

/* ============================ END OF FILE ====================== */
