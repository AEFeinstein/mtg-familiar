/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.util;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Represents a set of character entity references defined by the
 * HTML 4.0 standard.
 *
 * <p>A complete description of the HTML 4.0 character set can be found
 * at https://www.w3.org/TR/html4/charset.html.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @author Craig Andrews
 * @since 1.2.1
 */
class HtmlCharacterEntityReferences {

	private static final String HtmlCharacterEntityReferencesProperties = "# Character Entity References defined by the HTML 4.0 standard.\n# A complete description of the HTML 4.0 character set can be found at:\n# http://www.w3.org/TR/html4/charset.html\n\n# Character entity references for ISO 8859-1 characters\n\n160 = nbsp\n161 = iexcl\n162 = cent\n163 = pound\n164 = curren\n165 = yen\n166 = brvbar\n167 = sect\n168 = uml\n169 = copy\n170 = ordf\n171 = laquo\n172 = not\n173 = shy\n174 = reg\n175 = macr\n176 = deg\n177 = plusmn\n178 = sup2\n179 = sup3\n180 = acute\n181 = micro\n182 = para\n183 = middot\n184 = cedil\n185 = sup1\n186 = ordm\n187 = raquo\n188 = frac14\n189 = frac12\n190 = frac34\n191 = iquest\n192 = Agrave\n193 = Aacute\n194 = Acirc\n195 = Atilde\n196 = Auml\n197 = Aring\n198 = AElig\n199 = Ccedil\n200 = Egrave\n201 = Eacute\n202 = Ecirc\n203 = Euml\n204 = Igrave\n205 = Iacute\n206 = Icirc\n207 = Iuml\n208 = ETH\n209 = Ntilde\n210 = Ograve\n211 = Oacute\n212 = Ocirc\n213 = Otilde\n214 = Ouml\n215 = times\n216 = Oslash\n217 = Ugrave\n218 = Uacute\n219 = Ucirc\n220 = Uuml\n221 = Yacute\n222 = THORN\n223 = szlig\n224 = agrave\n225 = aacute\n226 = acirc\n227 = atilde\n228 = auml\n229 = aring\n230 = aelig\n231 = ccedil\n232 = egrave\n233 = eacute\n234 = ecirc\n235 = euml\n236 = igrave\n237 = iacute\n238 = icirc\n239 = iuml\n240 = eth\n241 = ntilde\n242 = ograve\n243 = oacute\n244 = ocirc\n245 = otilde\n246 = ouml\n247 = divide\n248 = oslash\n249 = ugrave\n250 = uacute\n251 = ucirc\n252 = uuml\n253 = yacute\n254 = thorn\n255 = yuml\n\n# Character entity references for symbols, mathematical symbols, and Greek letters\n\n402 = fnof\n913 = Alpha\n914 = Beta\n915 = Gamma\n916 = Delta\n917 = Epsilon\n918 = Zeta\n919 = Eta\n920 = Theta\n921 = Iota\n922 = Kappa\n923 = Lambda\n924 = Mu\n925 = Nu\n926 = Xi\n927 = Omicron\n928 = Pi\n929 = Rho\n931 = Sigma\n932 = Tau\n933 = Upsilon\n934 = Phi\n935 = Chi\n936 = Psi\n937 = Omega\n945 = alpha\n946 = beta\n947 = gamma\n948 = delta\n949 = epsilon\n950 = zeta\n951 = eta\n952 = theta\n953 = iota\n954 = kappa\n955 = lambda\n956 = mu\n957 = nu\n958 = xi\n959 = omicron\n960 = pi\n961 = rho\n962 = sigmaf\n963 = sigma\n964 = tau\n965 = upsilon\n966 = phi\n967 = chi\n968 = psi\n969 = omega\n977 = thetasym\n978 = upsih\n982 = piv\n8226 = bull\n8230 = hellip\n8242 = prime\n8243 = Prime\n8254 = oline\n8260 = frasl\n8472 = weierp\n8465 = image\n8476 = real\n8482 = trade\n8501 = alefsym\n8592 = larr\n8593 = uarr\n8594 = rarr\n8595 = darr\n8596 = harr\n8629 = crarr\n8656 = lArr\n8657 = uArr\n8658 = rArr\n8659 = dArr\n8660 = hArr\n8704 = forall\n8706 = part\n8707 = exist\n8709 = empty\n8711 = nabla\n8712 = isin\n8713 = notin\n8715 = ni\n8719 = prod\n8721 = sum\n8722 = minus\n8727 = lowast\n8730 = radic\n8733 = prop\n8734 = infin\n8736 = ang\n8743 = and\n8744 = or\n8745 = cap\n8746 = cup\n8747 = int\n8756 = there4\n8764 = sim\n8773 = cong\n8776 = asymp\n8800 = ne\n8801 = equiv\n8804 = le\n8805 = ge\n8834 = sub\n8835 = sup\n8836 = nsub\n8838 = sube\n8839 = supe\n8853 = oplus\n8855 = otimes\n8869 = perp\n8901 = sdot\n8968 = lceil\n8969 = rceil\n8970 = lfloor\n8971 = rfloor\n9001 = lang\n9002 = rang\n9674 = loz\n9824 = spades\n9827 = clubs\n9829 = hearts\n9830 = diams\n\n# Character entity references for markup-significant and internationalization characters\n\n34 = quot\n38 = amp\n39 = #39\n60 = lt\n62 = gt\n338 = OElig\n339 = oelig\n352 = Scaron\n353 = scaron\n376 = Yuml\n710 = circ\n732 = tilde\n8194 = ensp\n8195 = emsp\n8201 = thinsp\n8204 = zwnj\n8205 = zwj\n8206 = lrm\n8207 = rlm\n8211 = ndash\n8212 = mdash\n8216 = lsquo\n8217 = rsquo\n8218 = sbquo\n8220 = ldquo\n8221 = rdquo\n8222 = bdquo\n8224 = dagger\n8225 = Dagger\n8240 = permil\n8249 = lsaquo\n8250 = rsaquo\n8364 = euro\n";

	private static final String PROPERTIES_FILE = "HtmlCharacterEntityReferences.properties";

	static final char REFERENCE_START = '&';

	static final String DECIMAL_REFERENCE_START = "&#";

	static final String HEX_REFERENCE_START = "&#x";

	static final char REFERENCE_END = ';';

	static final char CHAR_NULL = (char) -1;


	private final String[] characterToEntityReferenceMap = new String[3000];

	private final Map<String, Character> entityReferenceToCharacterMap = new HashMap<>(512);


	/**
	 * Returns a new set of character entity references reflecting the HTML 4.0 character set.
	 */
	public HtmlCharacterEntityReferences() {
		Properties entityReferences = new Properties();

		// Load reference definition file
		InputStream is = new ByteArrayInputStream(HtmlCharacterEntityReferencesProperties.getBytes(StandardCharsets.UTF_8));
		if (is == null) {
			throw new IllegalStateException(
					"Cannot find reference definition file [HtmlCharacterEntityReferences.properties] as class path resource");
		}
		try {
			try {
				entityReferences.load(is);
			}
			finally {
				is.close();
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Failed to parse reference definition file [HtmlCharacterEntityReferences.properties]: " + ex.getMessage());
		}

		// Parse reference definition properties
		Enumeration<?> keys = entityReferences.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			int referredChar = Integer.parseInt(key);
			// Assert.isTrue((referredChar < 1000 || (referredChar >= 8000 && referredChar < 10000)),
			// 		() -> "Invalid reference to special HTML entity: " + referredChar);
			int index = (referredChar < 1000 ? referredChar : referredChar - 7000);
			String reference = entityReferences.getProperty(key);
			this.characterToEntityReferenceMap[index] = REFERENCE_START + reference + REFERENCE_END;
			this.entityReferenceToCharacterMap.put(reference, (char) referredChar);
		}
	}


	/**
	 * Return the number of supported entity references.
	 */
	public int getSupportedReferenceCount() {
		return this.entityReferenceToCharacterMap.size();
	}

	/**
	 * Return true if the given character is mapped to a supported entity reference.
	 */
	public boolean isMappedToReference(char character) {
		return isMappedToReference(character, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * Return true if the given character is mapped to a supported entity reference.
	 */
	public boolean isMappedToReference(char character, String encoding) {
		return (convertToReference(character, encoding) != null);
	}

	/**
	 * Return the reference mapped to the given character, or {@code null} if none found.
	 */
	@Nullable
	public String convertToReference(char character) {
		return convertToReference(character, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * Return the reference mapped to the given character, or {@code null} if none found.
	 * @since 4.1.2
	 */
	@Nullable
	public String convertToReference(char character, String encoding) {
		if (encoding.startsWith("UTF-")){
			switch (character){
				case '<' : return "&lt;";
				case '>' : return "&gt;";
				case '"' : return "&quot;";
				case '&' : return "&amp;";
				case '\'' : return "&#39;";
				default : return null;
			}
		}
		else if (character < 1000 || (character >= 8000 && character < 10000)) {
			int index = (character < 1000 ? character : character - 7000);
			String entityReference = this.characterToEntityReferenceMap[index];
			if (entityReference != null) {
				return entityReference;
			}
		}
		return null;
	}

	/**
	 * Return the char mapped to the given entityReference or -1.
	 */
	public char convertToCharacter(String entityReference) {
		Character referredCharacter = this.entityReferenceToCharacterMap.get(entityReference);
		if (referredCharacter != null) {
			return referredCharacter;
		}
		return CHAR_NULL;
	}

}
