package eu.scape_project.roda.core.plan;

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

	public BasicLuceneQueryTranslator() {
		qp = new MultiFieldQueryParser(Version.LUCENE_44,
				PlanManager.planFields, new StandardAnalyzer(Version.LUCENE_44));
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

			String index = ctn.getResultSetName();

			// String newIndex = (String) indexMappings.get(index);

			// if (newIndex != null) {
			// index = newIndex;
			// }

			if (StringUtils.isNotBlank(index)) {
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
				logger.debug("term(qualifier=\"" + ctn.getResultSetName()
						+ "\" relation=\"" + ctn.getRelation().getBase()
						+ "\" term=\"" + ctn.getTerm() + "\")");
		} else if (logger.isDebugEnabled())
			logger.debug("UnknownCQLNode(" + node + ")");
	}

	// public Query makeQuery(CQLNode node) throws SRWDiagnostic{
	// return makeQuery(node, null);
	// }
	//
	// Query makeQuery(CQLNode node, Query leftQuery) throws SRWDiagnostic{
	// Query query = null;
	//
	// if(node instanceof CQLBooleanNode) {
	// CQLBooleanNode cbn=(CQLBooleanNode)node;
	//
	// Query left = makeQuery(cbn.left);
	// Query right = makeQuery(cbn.right, left);
	//
	// if(node instanceof CQLAndNode) {
	// if (left instanceof BooleanQuery) {
	// query = left;
	// log.info("  Anding left and right");
	// AndQuery((BooleanQuery) left, right);
	// } else {
	// query = new BooleanQuery();
	// log.info("  Anding left and right in new query");
	// AndQuery((BooleanQuery) query, left);
	// AndQuery((BooleanQuery) query, right);
	// }
	//
	// } else if(node instanceof CQLNotNode) {
	//
	// if (left instanceof BooleanQuery) {
	// log.debug("  Notting left and right");
	// query = left;
	// NotQuery((BooleanQuery) left, right);
	// } else {
	// query = new BooleanQuery();
	// log.debug("  Notting left and right in new query");
	// AndQuery((BooleanQuery) query, left);
	// NotQuery((BooleanQuery) query, right);
	// }
	//
	// } else if(node instanceof CQLOrNode) {
	// if (left instanceof BooleanQuery) {
	// log.debug("  Or'ing left and right");
	// query = left;
	// OrQuery((BooleanQuery) left, right);
	// } else {
	// log.debug("  Or'ing left and right in new query");
	// query = new BooleanQuery();
	// OrQuery((BooleanQuery) query, left);
	// OrQuery((BooleanQuery) query, right);
	// }
	// } else {
	// throw new RuntimeException("Unknown boolean");
	// }
	//
	// } else if(node instanceof CQLTermNode) {
	// CQLTermNode ctn=(CQLTermNode)node;
	//
	// String relation = ctn.getRelation().getBase();
	// String index=ctn.getQualifier();
	//
	// if (!index.equals("")) {
	// if(relation.equals("=") || relation.equals("scr")) {
	// query = createTermQuery(index, ctn.getTerm(), relation);
	// } else if (relation.equals("<")) {
	// term = new Term(index, ctn.getTerm());
	// //term is upperbound, exclusive
	// query = new RangeQuery(null,term,false);
	// } else if (relation.equals(">")) {
	// term = new Term(index, ctn.getTerm());
	// //term is lowerbound, exclusive
	// query = new RangeQuery(term,null,false);
	// } else if (relation.equals("<=")) {
	// term = new Term(index, ctn.getTerm());
	// //term is upperbound, inclusive
	// query = new RangeQuery(null,term,true);
	// } else if (relation.equals(">=")) {
	// term = new Term(index, ctn.getTerm());
	// //term is lowebound, inclusive
	// query = new RangeQuery(term,null,true);
	//
	// } else if (relation.equals("<>")) {
	// /**
	// * <> is an implicit NOT.
	// *
	// * For example the following statements are identical results:
	// * foo=bar and zoo<>xar
	// * foo=bar not zoo=xar
	// */
	//
	// if (leftQuery == null) {
	// // first term in query create an empty Boolean query to NOT
	// query = new BooleanQuery();
	// } else {
	// if (leftQuery instanceof BooleanQuery) {
	// // left query is already a BooleanQuery use it
	// query = leftQuery;
	// } else {
	// // left query was not a boolean, create a boolean query
	// // and AND the left query to it
	// query = new BooleanQuery();
	// AndQuery((BooleanQuery)query, leftQuery);
	// }
	// }
	// //create a term query for the term then NOT it to the boolean query
	// Query termQuery = createTermQuery(index,ctn.getTerm(), relation);
	// NotQuery((BooleanQuery) query, termQuery);
	//
	// } else if (relation.equals("any")) {
	// //implicit or
	// query = createTermQuery(index,ctn.getTerm(), relation);
	//
	// } else if (relation.equals("all")) {
	// //implicit and
	// query = createTermQuery(index,ctn.getTerm(), relation);
	// } else if (relation.equals("exact")) {
	// /**
	// * implicit and. this query will only return accurate
	// * results for indexes that have been indexed using
	// * a non-tokenizing analyzer
	// */
	// query = createTermQuery(index,ctn.getTerm(), relation);
	// } else {
	// //anything else is unsupported
	// throw new SRWDiagnostic(19, ctn.getRelation().getBase());
	// }
	//
	// }
	// } else {
	// throw new SRWDiagnostic(47, "UnknownCQLNode: "+node+")");
	// }
	// if (query != null) {
	// log.info("Query : " + query.toString());
	// }
	// return query;
	// }
	//
	// Query createTermQuery(String cqlIndexName, String value, String relation)
	// throws SRWDiagnostic {
	//
	// Query termQuery = null;
	//
	// // map the cqlIndexName to a lucene index
	// String index=(String)indexMappings.get(cqlIndexName);
	// if(index==null)
	// throw new SRWDiagnostic(SRWDiagnostic.UnsupportedIndex, cqlIndexName);
	//
	// /**
	// * check to see if there are any spaces. If there are spaces each
	// * word must be broken into a single term search and then all queries
	// * must be combined using an and.
	// */
	// if (value.indexOf(" ") == -1) {
	// // no space found, just create a single term search
	// //todo case insensitivity?
	// term = new Term(index, value);
	// if (value.indexOf("?") != -1 || value.indexOf("*")!=-1 ){
	// termQuery = new WildcardQuery(term);
	// } else {
	// termQuery = new TermQuery(term);
	// }
	//
	// } else {
	// // space found, iterate through the terms to create a multiterm search
	//
	// if (relation == null || relation.equals("=") || relation.equals("<>") ||
	// relation.equals("exact")) {
	// /**
	// * default is =, all terms must be next to eachother.
	// * <> uses = as its term query.
	// * exact is a phrase query
	// */
	// PhraseQuery phraseQuery = new PhraseQuery();
	// StringTokenizer tokenizer = new StringTokenizer(value, " ");
	// while (tokenizer.hasMoreTokens()) {
	// String curValue = tokenizer.nextToken();
	// phraseQuery.add(new Term(index, curValue));
	// }
	// termQuery = phraseQuery;
	//
	// } else if(relation.equals("any")) {
	// /**
	// * any is an implicit OR
	// */
	// termQuery = new BooleanQuery();
	// StringTokenizer tokenizer = new StringTokenizer(value, " ");
	// while (tokenizer.hasMoreTokens()) {
	// String curValue = tokenizer.nextToken();
	// Query subSubQuery = createTermQuery(cqlIndexName, curValue, relation);
	// OrQuery((BooleanQuery) termQuery, subSubQuery);
	// }
	//
	// } else if (relation.equals("all")) {
	// /**
	// * any is an implicit AND
	// */
	// termQuery = new BooleanQuery();
	// StringTokenizer tokenizer = new StringTokenizer(value, " ");
	// while (tokenizer.hasMoreTokens()) {
	// String curValue = tokenizer.nextToken();
	// Query subSubQuery = createTermQuery(cqlIndexName, curValue, relation);
	// AndQuery((BooleanQuery) termQuery, subSubQuery);
	// }
	// }
	//
	// }
	//
	// return termQuery;
	// }
	//
	// /**
	// * Join the two queries together with boolean AND
	// * @param query
	// * @param query2
	// */
	// void AndQuery(BooleanQuery query, Query query2) {
	// /**
	// * required = true (must match sub query)
	// * prohibited = false (does not need to NOT match sub query)
	// */
	// query.add(query2, BooleanClause.Occur.MUST);
	// }
	//
	// void OrQuery(BooleanQuery query, Query query2) {
	// /**
	// * required = false (does not need to match sub query)
	// * prohibited = false (does not need to NOT match sub query)
	// */
	// query.add(query2, BooleanClause.Occur.SHOULD);
	// }
	//
	// void NotQuery(BooleanQuery query, Query query2) {
	// /**
	// * required = false (does not need to match sub query)
	// * prohibited = true (must not match sub query)
	// */
	// query.add(query2, BooleanClause.Occur.MUST_NOT);
	// }
	//
	// void dumpQueryTree(CQLNode node) {
	// if(node instanceof CQLBooleanNode) {
	// CQLBooleanNode cbn=(CQLBooleanNode)node;
	// dumpQueryTree(cbn.left);
	// if(node instanceof CQLAndNode)
	// log.info(" AND ");
	// else if(node instanceof CQLNotNode)
	// log.info(" NOT ");
	// else if(node instanceof CQLOrNode)
	// log.info(" OR ");
	// else log.info(" UnknownBoolean("+cbn+") ");
	// dumpQueryTree(cbn.right);
	// }
	// else if(node instanceof CQLTermNode) {
	// CQLTermNode ctn=(CQLTermNode)node;
	// log.info("term(qualifier=\""+ctn.getQualifier()+"\" relation=\""+
	// ctn.getRelation().getBase()+"\" term=\""+ctn.getTerm()+"\")");
	// }
	// else log.info("UnknownCQLNode("+node+")");
	// }
}
