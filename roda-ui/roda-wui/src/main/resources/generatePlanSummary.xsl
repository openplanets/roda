<!-- 
   ==========================================================
   Stylesheet to transform SPARQL results from http://rdf.myexperiment.org/sparql to 
   list of components
   ==========================================================
   ==========================================================

--> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:sparql="http://www.w3.org/2005/sparql-results#"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:plato="http://ifs.tuwien.ac.at/dp/plato"
    exclude-result-prefixes="java xalan xsi sparql plato">

<xsl:output method="html" indent="yes" encoding="UTF-8" />
<xsl:param name="downloadURL" />
<xsl:param name="workflowSVG" />

<xsl:template match="plato:plans">
<html>
<head>
	<title>Preservation Plan</title>
	<link rel="stylesheet" href="plan.css"/>
</head>
<body>

<div class="container">
	<div class="banner"></div>
	<div class="dataContainer">
	<xsl:apply-templates/>
	</div>
</div>	
</body>

</html>	
</xsl:template>


<xsl:template match="*|text()|@*">
</xsl:template>


<xsl:template match="plato:plan">
	<h1>Plan: <xsl:value-of select="plato:properties/@name"/><a href="{$downloadURL}" target="_BLANK">download</a></h1>
	<h2>Basis</h2>
	<fieldset>
		<div class="field">
			<div class="fieldname">Identification Code:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:basis/@identificationCode"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Repository Identifier:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:properties/@repositoryIdentifier"/></div>
		</div>
		<hr/>
		<div class="field">
			<div class="fieldname">Responsible Planners:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:properties/@author"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Organisation:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:properties/@organization"/></div>
		</div>
	</fieldset>
	<xsl:apply-templates/>

</xsl:template>

<xsl:template match="plato:sampleRecords">
	<h2>Collection</h2>
	<fieldset>
		<div class="field">
			<div class="fieldname">Collection ID:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:collectionProfile/plato:collectionID"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Number of objects:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:collectionProfile/plato:numberOfObjects"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Description:</div>
			<div class="fieldvaluetext"><xsl:value-of select="plato:samplesDescription"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Type of Objects:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:collectionProfile/plato:typeOfObjects"/></div>
		</div>
	</fieldset>

	
	<h2>Records</h2>
	<xsl:for-each select="plato:record">
		<fieldset>
			<div class="field">
				<div class="fieldname">Full name:</div>
				<div class="fieldvalue"><xsl:value-of select="@fullname"/></div>
			</div>
			<div class="field">
				<div class="fieldname">Short name:</div>
				<div class="fieldvalue"><xsl:value-of select="@shortName"/></div>
			</div>
			<div class="field">
				<div class="fieldname">Content type:</div>
				<div class="fieldvalue"><xsl:value-of select="@contentType"/></div>
			</div>
			<fieldset>
				<legend>Format info</legend>
				<div class="field">
					<div class="fieldname">PUID:</div>
					<div class="fieldvalue"><xsl:value-of select="plato:formatInfo/@puid"/></div>
				</div>
				<div class="field">
					<div class="fieldname">Name:</div>
					<div class="fieldvalue"><xsl:value-of select="plato:formatInfo/@name"/></div>
				</div>
				<div class="field">
					<div class="fieldname">Version:</div>
					<div class="fieldvalue"><xsl:value-of select="plato:formatInfo/@version"/></div>
				</div>
				<div class="field">
					<div class="fieldname">MimeType:</div>
					<div class="fieldvalue"><xsl:value-of select="plato:formatInfo/@mimeType"/></div>
				</div>
			</fieldset>
		</fieldset>
	</xsl:for-each>
</xsl:template>

<xsl:template match="plato:preservationActionPlan">
	<h2>Preservation Action Plan</h2>
	<fieldset>
		<xsl:value-of select="$workflowSVG" disable-output-escaping="yes" />
	</fieldset>
	
</xsl:template>

<xsl:template match="plato:requirementsDefinition">
	<h2>Requirements definition</h2>
	<fieldset>
		<div class="field">
			<div class="fieldname">Description:</div>
			<div class="fieldvaluetext"><xsl:value-of select="plato:description"/></div>
		</div>
	</fieldset>
</xsl:template>

<xsl:template match="plato:alternatives">
	<h2>Alternatives</h2>
	<xsl:apply-templates/>	
</xsl:template>
<xsl:template match="plato:alternative[@name = ../../plato:recommendation/@alternativeName]">
	<fieldset>
		<h3><b>Selected Alternative: </b> <xsl:value-of select="@name"/></h3>
		<div class="field">
			<div class="fieldname">Description:</div>
			<div class="fieldvaluetext"><xsl:value-of select="plato:description"/></div>
		</div>
		<hr/>
		<div class="field">
			<div class="fieldname">Reasoning:</div>
			<div class="fieldvalue"><xsl:value-of select="../../plato:recommendation/plato:reasoning"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Effects of applying recommended solution:</div>
			<div class="fieldvalue"><xsl:value-of select="../../plato:recommendation/plato:effects"/></div>
		</div>
	</fieldset>



	<hr/>
</xsl:template>

<xsl:template match="plato:alternative[@name != ../../plato:recommendation/@alternativeName]">
	<fieldset>
		<h3>Alternative: <xsl:value-of select="@name"/> <xsl:if test="@discarded ='true'"><b> - discarded</b> </xsl:if></h3>
		<div class="field">
			<div class="fieldname">Description:</div>
			<div class="fieldvaluetext"><xsl:value-of select="plato:description"/></div>
		</div>
	</fieldset>
</xsl:template>

<xsl:template match="plato:decision">
	<h2>Decision</h2>
	<fieldset>
		<div class="field">
			<div class="fieldname">Reason:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:reason"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Action needed:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:actionNeeded"/></div>
		</div>
	</fieldset>
</xsl:template>

<xsl:template match="plato:evaluation">
	<h2>Evaluation</h2>
	<fieldset>
		<div class="field">
			<div class="fieldname">Comment:</div>
			<div class="fieldvaluetext"><xsl:value-of select="plato:comment"/></div>
		</div>
	</fieldset>
</xsl:template>

<xsl:template match="plato:recommendation">
	<h2>Recommendation</h2>
	<fieldset>
		<div class="field">
			<div class="fieldname">Alternative name:</div>
			<div class="fieldvalue"><xsl:value-of select="@alternativeName"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Reasoning:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:reasoning"/></div>
		</div>
		<div class="field">
			<div class="fieldname">Effects:</div>
			<div class="fieldvalue"><xsl:value-of select="plato:effects"/></div>
		</div>
	</fieldset>
</xsl:template>


</xsl:stylesheet>