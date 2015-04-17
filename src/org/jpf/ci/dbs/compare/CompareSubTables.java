/** 
 * @author ��ƽ�� 
 * E-mail:wupf@asiainfo.com 
 * @version ����ʱ�䣺2015��2��8�� ����10:09:38 
 * ��˵�� 
 */

package org.jpf.ci.dbs.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 */
public class CompareSubTables extends AbstractDbCompare
{
	private static final Logger logger = LogManager.getLogger();

    private void  CheckSubTables(Map<String, Table> map_dev)
	{
		
    	for (Iterator iter_table = map_dev.keySet().iterator(); iter_table.hasNext();)
		{
			String key_table = (String) iter_table.next();
			
			if (!CompareUtil.IsSubTable(key_table))
			{
				//分表
				//logger.info(key_table);
				iter_table.remove();
			}
		}
    	
	}
  
	public void DoWork(Connection conn_pdm, Connection conn_develop) throws Exception
	{

		// PDM数据库连接
		Map<String, Table> map_pdm = GetTables(conn_pdm);
		// 开发数据库连接

		Map<String, Table> map_develop = GetTables(conn_develop);
		logger.info(map_develop.size());
		CheckSubTables(map_develop);
		logger.info(map_develop.size());
		
		// 遍历开发库Map
		for (Iterator iter_table = map_develop.keySet().iterator(); iter_table.hasNext();)
		{
			String key_table = (String) iter_table.next();
   
			Table table_develop = map_develop.get(key_table);// 获得PDM中的表
			key_table=CompareUtil. GetParentTableName(key_table);
			Table table_pdm = map_pdm.get(key_table);// 尝试从比对库中获得同名表
			if (table_pdm == null)
			{ // 如果获得表为空，说明PDM不存在，比对库存在
				CompareUtil.append(table_develop, null, null,2, sb);
				iCount2++;
			} else
			{ // 表相同，判断字段、字段类型、字段长度
				for (Iterator iter_column = table_develop.columns.keySet().iterator(); iter_column.hasNext();)
				{
					String key_column = (String) iter_column.next();
					Column column_develop = (Column) table_develop.columns.get(key_column);// 获得开发库中的列
					Column column_pdm = (Column) table_pdm.columns.get(key_column);// 尝试从生产库中获得同名列
					if (column_pdm == null)
					{// 如果列名为空，说明PDM不存在，比对库存在
						CompareUtil.append(table_develop, column_develop, null,4, sb);
						iCount4++;
					} else
					{// 说明两者都存在
						if (!column_develop.getDataType().equalsIgnoreCase(column_pdm.getDataType()))// 字段类型不一致
						{	CompareUtil.append(table_develop, column_pdm,column_develop, 5, sb);
						    iCount5++;
						}
						 if (!column_develop.getNullable().equalsIgnoreCase(column_pdm.getNullable()))// 是否为空不一致
						 {	
							 CompareUtil.append(table_develop, column_pdm, column_develop,6, sb);
						     iCount6++;
						 }
					}
				}
			}
		}

	}

	public Map<String, Table> GetTables(Connection transaction) throws Exception
	{
		String sSql = " select TABLE_NAME,COLUMN_NAME,IS_NULLABLE,COLUMN_TYPE,COLUMN_comment from information_schema.COLUMNS where table_schema=? ";
		if (strExcludeTable !=null && strExcludeTable.length()>0)
		{
			sSql+=" and table_name not like %'"+strExcludeTable+"%' ";
		}
		sSql+=" order By table_name,column_name";
		PreparedStatement pstmt = transaction.prepareStatement(sSql);
		pstmt.setString(1, strDomain);
		logger.debug(sSql);
		System.out.println("Domain="+strDomain);

		ResultSet rs = pstmt.executeQuery();
		Map<String, Table> map = new HashMap<String, Table>();
		String tableName = "";
		Table table = null;
		while (rs.next())
		{
			if (!tableName.equals(rs.getString("table_name").toLowerCase().trim()))
			{// һ���±�
				tableName = rs.getString("table_name").toLowerCase().trim();
				table = new Table(tableName);
				Column column = new Column(rs.getString("Column_Name").toLowerCase().trim(),
						rs.getString("COLUMN_TYPE").toLowerCase().trim(), rs.getString("IS_NULLABLE").toLowerCase().trim(), rs.getString("COLUMN_comment"));
				table.columns.put(column.getColumnName(), column);
				map.put(tableName, table);
			} else
			{// �Ѵ��ڵı������ֶ�
				Column column = new Column(rs.getString("Column_Name").toLowerCase().trim(),
						rs.getString("COLUMN_TYPE").toLowerCase().trim(), rs.getString("IS_NULLABLE").toLowerCase().trim(), rs.getString("COLUMN_comment"));
				table.columns.put(column.getColumnName(), column);
			}
		}
		if (null != rs)
			rs.close();
		// transaction.finalize();
		return map;
	}

	/* (non-Javadoc)
	 * @see org.jpf.ci.dbs.compare.AbstractDbCompare#GetHtmlName()
	 */
	@Override
	String GetHtmlName()
	{
		// TODO Auto-generated method stub
		return "compare_table.html";
	}
}
