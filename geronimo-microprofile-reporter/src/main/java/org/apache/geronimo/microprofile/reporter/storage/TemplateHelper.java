/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.microprofile.reporter.storage;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TemplateHelper {
    private final Map<Character, String> escaped = new HashMap<Character, String>() {
        {
            put('"', "&quot;");
            put('&', "&amp;");
            put('<', "&lt;");
            put('>', "&gt;");
            put('\u00A0', "&nbsp;");
            put('\u00A1', "&iexcl;");
            put('\u00A2', "&cent;");
            put('\u00A3', "&pound;");
            put('\u00A4', "&curren;");
            put('\u00A5', "&yen;");
            put('\u00A6', "&brvbar;");
            put('\u00A7', "&sect;");
            put('\u00A8', "&uml;");
            put('\u00A9', "&copy;");
            put('\u00AA', "&ordf;");
            put('\u00AB', "&laquo;");
            put('\u00AC', "&not;");
            put('\u00AD', "&shy;");
            put('\u00AE', "&reg;");
            put('\u00AF', "&macr;");
            put('\u00B0', "&deg;");
            put('\u00B1', "&plusmn;");
            put('\u00B2', "&sup2;");
            put('\u00B3', "&sup3;");
            put('\u00B4', "&acute;");
            put('\u00B5', "&micro;");
            put('\u00B6', "&para;");
            put('\u00B7', "&middot;");
            put('\u00B8', "&cedil;");
            put('\u00B9', "&sup1;");
            put('\u00BA', "&ordm;");
            put('\u00BB', "&raquo;");
            put('\u00BC', "&frac14;");
            put('\u00BD', "&frac12;");
            put('\u00BE', "&frac34;");
            put('\u00BF', "&iquest;");
            put('\u00C0', "&Agrave;");
            put('\u00C1', "&Aacute;");
            put('\u00C2', "&Acirc;");
            put('\u00C3', "&Atilde;");
            put('\u00C4', "&Auml;");
            put('\u00C5', "&Aring;");
            put('\u00C6', "&AElig;");
            put('\u00C7', "&Ccedil;");
            put('\u00C8', "&Egrave;");
            put('\u00C9', "&Eacute;");
            put('\u00CA', "&Ecirc;");
            put('\u00CB', "&Euml;");
            put('\u00CC', "&Igrave;");
            put('\u00CD', "&Iacute;");
            put('\u00CE', "&Icirc;");
            put('\u00CF', "&Iuml;");
            put('\u00D0', "&ETH;");
            put('\u00D1', "&Ntilde;");
            put('\u00D2', "&Ograve;");
            put('\u00D3', "&Oacute;");
            put('\u00D4', "&Ocirc;");
            put('\u00D5', "&Otilde;");
            put('\u00D6', "&Ouml;");
            put('\u00D7', "&times;");
            put('\u00D8', "&Oslash;");
            put('\u00D9', "&Ugrave;");
            put('\u00DA', "&Uacute;");
            put('\u00DB', "&Ucirc;");
            put('\u00DC', "&Uuml;");
            put('\u00DD', "&Yacute;");
            put('\u00DE', "&THORN;");
            put('\u00DF', "&szlig;");
            put('\u00E0', "&agrave;");
            put('\u00E1', "&aacute;");
            put('\u00E2', "&acirc;");
            put('\u00E3', "&atilde;");
            put('\u00E4', "&auml;");
            put('\u00E5', "&aring;");
            put('\u00E6', "&aelig;");
            put('\u00E7', "&ccedil;");
            put('\u00E8', "&egrave;");
            put('\u00E9', "&eacute;");
            put('\u00EA', "&ecirc;");
            put('\u00EB', "&euml;");
            put('\u00EC', "&igrave;");
            put('\u00ED', "&iacute;");
            put('\u00EE', "&icirc;");
            put('\u00EF', "&iuml;");
            put('\u00F0', "&eth;");
            put('\u00F1', "&ntilde;");
            put('\u00F2', "&ograve;");
            put('\u00F3', "&oacute;");
            put('\u00F4', "&ocirc;");
            put('\u00F5', "&otilde;");
            put('\u00F6', "&ouml;");
            put('\u00F7', "&divide;");
            put('\u00F8', "&oslash;");
            put('\u00F9', "&ugrave;");
            put('\u00FA', "&uacute;");
            put('\u00FB', "&ucirc;");
            put('\u00FC', "&uuml;");
            put('\u00FD', "&yacute;");
            put('\u00FE', "&thorn;");
            put('\u00FF', "&yuml;");
            put('\u0192', "&fnof;");
            put('\u0391', "&Alpha;");
            put('\u0392', "&Beta;");
            put('\u0393', "&Gamma;");
            put('\u0394', "&Delta;");
            put('\u0395', "&Epsilon;");
            put('\u0396', "&Zeta;");
            put('\u0397', "&Eta;");
            put('\u0398', "&Theta;");
            put('\u0399', "&Iota;");
            put('\u039A', "&Kappa;");
            put('\u039B', "&Lambda;");
            put('\u039C', "&Mu;");
            put('\u039D', "&Nu;");
            put('\u039E', "&Xi;");
            put('\u039F', "&Omicron;");
            put('\u03A0', "&Pi;");
            put('\u03A1', "&Rho;");
            put('\u03A3', "&Sigma;");
            put('\u03A4', "&Tau;");
            put('\u03A5', "&Upsilon;");
            put('\u03A6', "&Phi;");
            put('\u03A7', "&Chi;");
            put('\u03A8', "&Psi;");
            put('\u03A9', "&Omega;");
            put('\u03B1', "&alpha;");
            put('\u03B2', "&beta;");
            put('\u03B3', "&gamma;");
            put('\u03B4', "&delta;");
            put('\u03B5', "&epsilon;");
            put('\u03B6', "&zeta;");
            put('\u03B7', "&eta;");
            put('\u03B8', "&theta;");
            put('\u03B9', "&iota;");
            put('\u03BA', "&kappa;");
            put('\u03BB', "&lambda;");
            put('\u03BC', "&mu;");
            put('\u03BD', "&nu;");
            put('\u03BE', "&xi;");
            put('\u03BF', "&omicron;");
            put('\u03C0', "&pi;");
            put('\u03C1', "&rho;");
            put('\u03C2', "&sigmaf;");
            put('\u03C3', "&sigma;");
            put('\u03C4', "&tau;");
            put('\u03C5', "&upsilon;");
            put('\u03C6', "&phi;");
            put('\u03C7', "&chi;");
            put('\u03C8', "&psi;");
            put('\u03C9', "&omega;");
            put('\u03D1', "&thetasym;");
            put('\u03D2', "&upsih;");
            put('\u03D6', "&piv;");
            put('\u2022', "&bull;");
            put('\u2026', "&hellip;");
            put('\u2032', "&prime;");
            put('\u2033', "&Prime;");
            put('\u203E', "&oline;");
            put('\u2044', "&frasl;");
            put('\u2118', "&weierp;");
            put('\u2111', "&image;");
            put('\u211C', "&real;");
            put('\u2122', "&trade;");
            put('\u2135', "&alefsym;");
            put('\u2190', "&larr;");
            put('\u2191', "&uarr;");
            put('\u2192', "&rarr;");
            put('\u2193', "&darr;");
            put('\u2194', "&harr;");
            put('\u21B5', "&crarr;");
            put('\u21D0', "&lArr;");
            put('\u21D1', "&uArr;");
            put('\u21D2', "&rArr;");
            put('\u21D3', "&dArr;");
            put('\u21D4', "&hArr;");
            put('\u2200', "&forall;");
            put('\u2202', "&part;");
            put('\u2203', "&exist;");
            put('\u2205', "&empty;");
            put('\u2207', "&nabla;");
            put('\u2208', "&isin;");
            put('\u2209', "&notin;");
            put('\u220B', "&ni;");
            put('\u220F', "&prod;");
            put('\u2211', "&sum;");
            put('\u2212', "&minus;");
            put('\u2217', "&lowast;");
            put('\u221A', "&radic;");
            put('\u221D', "&prop;");
            put('\u221E', "&infin;");
            put('\u2220', "&ang;");
            put('\u2227', "&and;");
            put('\u2228', "&or;");
            put('\u2229', "&cap;");
            put('\u222A', "&cup;");
            put('\u222B', "&int;");
            put('\u2234', "&there4;");
            put('\u223C', "&sim;");
            put('\u2245', "&cong;");
            put('\u2248', "&asymp;");
            put('\u2260', "&ne;");
            put('\u2261', "&equiv;");
            put('\u2264', "&le;");
            put('\u2265', "&ge;");
            put('\u2282', "&sub;");
            put('\u2283', "&sup;");
            put('\u2284', "&nsub;");
            put('\u2286', "&sube;");
            put('\u2287', "&supe;");
            put('\u2295', "&oplus;");
            put('\u2297', "&otimes;");
            put('\u22A5', "&perp;");
            put('\u22C5', "&sdot;");
            put('\u2308', "&lceil;");
            put('\u2309', "&rceil;");
            put('\u230A', "&lfloor;");
            put('\u230B', "&rfloor;");
            put('\u2329', "&lang;");
            put('\u232A', "&rang;");
            put('\u25CA', "&loz;");
            put('\u2660', "&spades;");
            put('\u2663', "&clubs;");
            put('\u2665', "&hearts;");
            put('\u2666', "&diams;");
            put('\u0152', "&OElig;");
            put('\u0153', "&oelig;");
            put('\u0160', "&Scaron;");
            put('\u0161', "&scaron;");
            put('\u0178', "&Yuml;");
            put('\u02C6', "&circ;");
            put('\u02DC', "&tilde;");
            put('\u2002', "&ensp;");
            put('\u2003', "&emsp;");
            put('\u2009', "&thinsp;");
            put('\u200C', "&zwnj;");
            put('\u200D', "&zwj;");
            put('\u200E', "&lrm;");
            put('\u200F', "&rlm;");
            put('\u2013', "&ndash;");
            put('\u2014', "&mdash;");
            put('\u2018', "&lsquo;");
            put('\u2019', "&rsquo;");
            put('\u201A', "&sbquo;");
            put('\u201C', "&ldquo;");
            put('\u201D', "&rdquo;");
            put('\u201E', "&bdquo;");
            put('\u2020', "&dagger;");
            put('\u2021', "&Dagger;");
            put('\u2030', "&permil;");
            put('\u2039', "&lsaquo;");
            put('\u203A', "&rsaquo;");
            put('\u20AC', "&euro;");
        }
    };

    public String escape(final String raw) {
        final char[] chars = raw.toCharArray();
        return IntStream.range(0, chars.length)
                        .mapToObj(index -> escaped.getOrDefault(chars[index], String.valueOf(chars[index])))
                        .collect(joining());
    }
}
