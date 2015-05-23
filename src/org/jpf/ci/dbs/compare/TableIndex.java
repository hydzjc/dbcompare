/** 
* @author ��ƽ�� 
* E-mail:wupf@asiainfo.com 
* @version ����ʱ�䣺2015��1��15�� ����8:47:45 
* ��˵�� 
*/ 

package org.jpf.ci.dbs.compare;


/**
 * 
 */
public class TableIndex
{

	private String indexName;
	private String colNames="";
	private int iNON_UNIQUE=0;
	private String constraint_type="";
	
	public String getConstraint_type()
	{
		if (constraint_type!=null && constraint_type.equalsIgnoreCase("null"))
		{	
			return "";
		}
		if (constraint_type==null)
		{
			return "";
		}
		return constraint_type;
	}

	public void setConstraint_type(String constraint_type)
	{
		this.constraint_type = constraint_type;
	}

	public int getNON_UNIQUE()
	{
		return iNON_UNIQUE;
	}

	public void setNON_UNIQUE(final int inNON_UNIQUE)
	{
		iNON_UNIQUE = inNON_UNIQUE;
	}


	public String getIndexName()
	{
		return indexName;
	}

	public void setIndexName(String indexName)
	{
		this.indexName = indexName;
	}

	/**
	 * 
	 */
	public TableIndex( String indexName,final int inNON_UNIQUE)
	{
		// TODO Auto-generated constructor stub
		this.indexName = indexName;
		this.iNON_UNIQUE=inNON_UNIQUE;
	}
	
	public void AddColName(String columnName)
	{
		colNames+=columnName+",";

	}

	public String getColNames()
	{
		return colNames;
	}

	public void setColNames(String colNames)
	{
		this.colNames = colNames;
	}
}
