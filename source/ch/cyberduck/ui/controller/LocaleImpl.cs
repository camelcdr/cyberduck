﻿// 
// Copyright (c) 2010 Yves Langisch. All rights reserved.
// http://cyberduck.ch/
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
// 
// Bug fixes, suggestions and comments should be sent to:
// yves@cyberduck.ch
// 
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;
using ch.cyberduck.core;
using Ch.Cyberduck.Core;
using ch.cyberduck.core.i18n;
using org.apache.log4j;
using Path = System.IO.Path;

namespace Ch.Cyberduck.Ui.Controller
{
    public class LocaleImpl : Locale
    {
        private static readonly Logger Log = Logger.getLogger(typeof (LocaleImpl).Name);
        private static readonly Regex StringsRegex = new Regex("\"(.*)\" = \"(.*)\"", RegexOptions.Compiled);

        private readonly IDictionary<string, Dictionary<string, string>> _cache =
            new Dictionary<string, Dictionary<string, string>>();

        private readonly string _language = Preferences.instance().getProperty("application.language");
        private readonly DirectoryInfo _resourcesDirectory;

        public LocaleImpl()
        {
            _resourcesDirectory = new DirectoryInfo(Path.Combine(".", _language + ".lproj"));
        }

        private void ReadBundleIntoCache(string bundle)
        {
            string dictionary = Path.Combine(_resourcesDirectory.FullName, bundle + ".strings");
            Log.debug("Caching bundle " + bundle);

            if (File.Exists(dictionary))
            {
                using (StreamReader file = new StreamReader(dictionary))
                {
                    Dictionary<string, string> bundleDict = new Dictionary<string, string>();
                    _cache[bundle] = bundleDict;
                    string line;
                    while ((line = file.ReadLine()) != null)
                    {
                        if (StringsRegex.IsMatch(line))
                        {
                            Match match = StringsRegex.Match(line);
                            string key = match.Groups[1].Value;
                            string value = match.Groups[2].Value;
                            bundleDict[key] = value;
                        }
                    }
                }
            }
        }

        public override string get(string key, string table)
        {
            Dictionary<string, string> bundle;

            if (!_cache.TryGetValue(table, out bundle))
            {
                ReadBundleIntoCache(table);
                //try again
                if (!_cache.TryGetValue(table, out bundle))
                {
                    Log.warn(string.Format("Key '{0}' in bundle '{1}' not found", key, table));
                    return key;
                }
            }

            string value;
            return bundle.TryGetValue(key, out value) ? value : key;
        }

        public static void Register()
        {
            LocaleFactory.addFactory(ch.cyberduck.core.Factory.NATIVE_PLATFORM, new Factory());
        }

        private class Factory : LocaleFactory
        {
            protected override object create()
            {
                return new LocaleImpl();
            }
        }
    }
}