package ca.carleton.gcrc.couch.export.impl;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.carleton.gcrc.couch.app.Document;
import ca.carleton.gcrc.couch.export.DocumentRetrieval;
import ca.carleton.gcrc.couch.export.ExportFormat;
import ca.carleton.gcrc.couch.export.SchemaCache;
import ca.carleton.gcrc.couch.export.SchemaExportInfo;
import ca.carleton.gcrc.couch.export.SchemaExportProperty;

public class ExportFormatCSV implements ExportFormat {

	final protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private DocumentRetrieval retrieval = null;
	private SchemaCache schemaCache = null;
	private String schemaName;
	private SchemaExportInfo exportInfo;
	
	public ExportFormatCSV(
		SchemaCache schemaCache
		,DocumentRetrieval retrieval
		) throws Exception {
		this.schemaCache = schemaCache;
		this.retrieval = retrieval;
	}

	@Override
	public String getMimeType() {
		return "text/csv";
	}

	@Override
	public String getCharacterEncoding() {
		return "utf-8";
	}

	@Override
	public void outputExport(OutputStream os) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
		outputExport(osw);
		osw.flush();
	}

	public void outputExport(Writer writer) throws Exception {

		while( retrieval.hasNext() ){
			Document doc = retrieval.getNext();
			if( null != doc  ) {
				try{
					outputDocument(writer, doc);
				} catch(Exception e) {
					throw new Exception("Error exporting document: "+doc.getId(), e);
				}
			}
		}
	}

	private void outputDocument(Writer writer, Document doc) throws Exception {
		JSONObject jsonDoc = doc.getJSONObject();

		String docSchemaName = jsonDoc.optString("nunaliit_schema");
		if( null != docSchemaName ) {
			// Capture first schema name and associated export info
			if( null == schemaName ){
				exportInfo = schemaCache.getExportInfo(docSchemaName);
				if( null != exportInfo ){
					schemaName = docSchemaName;
					
					// Output first line
					List<Object> values = new Vector<Object>();
					for(SchemaExportProperty exportProperty : exportInfo.getProperties()){
						String value = exportProperty.getLabel();
						values.add(value);
					}
					printCsvLine(writer, values);
				}
			}
			
			// Output only if matches the schema being processed
			if( docSchemaName.equals(schemaName) ){
				List<Object> values = new Vector<Object>();
				for(SchemaExportProperty exportProperty : exportInfo.getProperties()){
					Object value = exportProperty.select(jsonDoc);
					values.add(value);
				}
				printCsvLine(writer, values);
			}
		}
	}
	
	private void printCsvLine(Writer writer, List<Object> values) throws Exception {
		boolean first = true;
		for(Object value : values){
			if( first ){
				first = false;
			} else {
				writer.write(",");
			}
			
			if( null == value ){
				writer.write("\"\"");
			
			} else if( value instanceof String ) {
				writer.write("\"");
				
				String str = (String)value;
				for(int loop=0; loop<str.length(); ++loop){
					char c = str.charAt(loop);
					switch(c){
					case '"':
						writer.write("\"\"");
						break;
					default:
						writer.write(c);
					}
				}
				
				writer.write("\"");
			
			} else if( value instanceof Number ) {
				Number n = (Number)value;
				writer.write( n.toString() );
				
			} else if( value instanceof Boolean ) {
				Boolean b = (Boolean)value;
				writer.write( b.toString() );
			}
		}
		
		writer.write("\n");
	}
}
