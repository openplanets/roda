package eu.scape_project.roda.core;

/* 
 * OCKHAM P2PREGISTRY Copyright 2006 Oregon State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

/**
 * @author peter Date: Oct 25, 2005 Time: 10:38:43 AM
 * @author Rui Castro
 */
public class BasicLuceneQueryTranslator {
	private static Logger logger = Logger
			.getLogger(BasicLuceneQueryTranslator.class);

	QueryParser qp = null;

	public BasicLuceneQueryTranslator(String[] defaultFields) {
		qp = new MultiFieldQueryParser(Version.LUCENE_44, defaultFields,
				new StandardAnalyzer(Version.LUCENE_44));
	}

	public Query makeQuery(CQLNode node) throws ParseException {

		if (logger.isDebugEnabled()) {
			dumpQueryTree(node);
		}

		StringBuffer sb = new StringBuffer();
		makeLuceneQuery(node, sb);
		return qp.parse(sb.toString());
	}

	private void makeLuceneQuery(CQLNode node, StringBuffer sb) {
		if (node instanceof CQLBooleanNode) {
			CQLBooleanNode cbn = (CQLBooleanNode) node;
			sb.append("(");
			makeLuceneQuery(cbn.getLeftOperand(), sb);
			if (node instanceof CQLAndNode)
				sb.append(" AND ");
			else if (node instanceof CQLNotNode)
				sb.append(" NOT ");
			else if (node instanceof CQLOrNode)
				sb.append(" OR ");
			else
				sb.append(" UnknownBoolean(" + cbn + ") ");
			makeLuceneQuery(cbn.getRightOperand(), sb);
			sb.append(")");
		} else if (node instanceof CQLTermNode) {
			CQLTermNode ctn = (CQLTermNode) node;

			String index = ctn.getIndex();

			if (StringUtils.isNotBlank(index)
					&& !"srw.serverChoice".equalsIgnoreCase(index)
					&& !"cql.serverChoice".equalsIgnoreCase(index)) {
				sb.append(index).append(":");
			}

			String term = ctn.getTerm();
			if (ctn.getRelation().getBase().equals("=")
					|| ctn.getRelation().getBase().equals("scr")) {
				if (term.indexOf(' ') >= 0)
					sb.append('"').append(term).append('"');
				else
					sb.append(ctn.getTerm());
			} else if (ctn.getRelation().getBase().equals("any")) {
				if (term.indexOf(' ') >= 0)
					sb.append('(').append(term).append(')');
				else
					sb.append(ctn.getTerm());
			} else if (ctn.getRelation().getBase().equals("all")) {
				if (term.indexOf(' ') >= 0) {
					sb.append('(');
					StringTokenizer st = new StringTokenizer(term);
					while (st.hasMoreTokens()) {
						sb.append(st.nextToken());
						if (st.hasMoreTokens())
							sb.append(" AND ");
					}
					sb.append(')');
				} else
					sb.append(ctn.getTerm());
			} else
				sb.append("Unsupported Relation: "
						+ ctn.getRelation().getBase());
		} else
			sb.append("UnknownCQLNode(" + node + ")");
	}

	private void dumpQueryTree(CQLNode node) {
		if (node instanceof CQLBooleanNode) {
			CQLBooleanNode cbn = (CQLBooleanNode) node;
			dumpQueryTree(cbn.getLeftOperand());
			if (node instanceof CQLAndNode)
				if (logger.isDebugEnabled())
					logger.debug(" AND ");
				else if (node instanceof CQLNotNode)
					if (logger.isDebugEnabled())
						logger.debug(" NOT ");
					else if (node instanceof CQLOrNode)
						if (logger.isDebugEnabled())
							logger.debug(" OR ");
						else if (logger.isDebugEnabled())
							logger.debug(" UnknownBoolean(" + cbn + ") ");
			dumpQueryTree(cbn.getRightOperand());
		} else if (node instanceof CQLTermNode) {
			CQLTermNode ctn = (CQLTermNode) node;
			if (logger.isDebugEnabled())
				logger.debug("term(qualifier=\"" + ctn.getIndex()
						+ "\" relation=\"" + ctn.getRelation().getBase()
						+ "\" term=\"" + ctn.getTerm() + "\")");
		} else if (logger.isDebugEnabled())
			logger.debug("UnknownCQLNode(" + node + ")");
	}

}
