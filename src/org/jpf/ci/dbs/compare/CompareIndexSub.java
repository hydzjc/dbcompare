/** 
 * @author 吴平福 
 * E-mail:wupf@asiainfo.com 
 * @version 创建时间：2015年1月15日 下午4:00:30 
 * 类说明 
 */

package org.jpf.ci.dbs.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jpf.utils.JpfDbUtils;
import org.jpf.visualwall.WallsDbConn;

public class CompareIndexSub extends AbstractDbCompare
{
	private static final Logger logger = LogManager.getLogger();

	public void DoWork(Connection conn_product, Connection conn_develop, CompareInfo cCompareInfo) throws Exception
	{
		String strPdmDomain = cCompareInfo.getStrDomain();
		if (strPdmDomain.startsWith("ud"))
		{
			strPdmDomain = "ud";
		}
		// 生产数据库连接
		LinkedHashMap<String, Table> map_pdm = GetTableIndexs(conn_product, strPdmDomain, cCompareInfo);
		// 开发数据库连接
		LinkedHashMap<String, Table> map_develop = GetTableIndexs(conn_develop, cCompareInfo.getStrDomain(),
				cCompareInfo);
		logger.info("begin compare index ");
		// 遍历开发库Map
		for (Iterator iter_table = map_develop.keySet().iterator(); iter_table.hasNext();)
		{
			String key_table = (String) iter_table.next();
			// 获得开发库中的表
			Table table_develop = map_develop.get(key_table);

			// 尝试从PDM库中获得同名表
			logger.info("dev_table=" + key_table);
			key_table = CompareUtil.GetParentTableName(key_table);
			logger.info("parent_table=" + key_table);
			Table table_pdm = map_pdm.get(key_table);// 尝试从比对库中获得同名表

			if (table_pdm != null)
			{

				for (Iterator iter_column = table_develop.indexs.keySet().iterator(); iter_column.hasNext();)
				{
					String key_index = (String) iter_column.next();
					if (key_index != null)
					{
						// 获得开发库中的索引
						TableIndex index_develop = (TableIndex) table_develop.indexs.get(key_index);
						// 尝试从PDM库中获得同名索引
						TableIndex index_pdm = (TableIndex) table_pdm.indexs.get(key_index);
						if (index_pdm == null)
						{// 如果索引名为空，说明开发存在，pdm不存在
							CompareUtil.appendIndex(table_develop, null, index_develop, 11, sb, vSql);
							iCount2++;
						} else
						{// 说明两者都存在

							if (!index_develop.getColNames().equalsIgnoreCase(index_pdm.getColNames()))
							{
								CompareUtil.appendIndex(table_develop, index_pdm, index_develop, 12, sb, vSql);
								iCount3++;
							}
							if (index_develop.getNON_UNIQUE() != index_pdm.getNON_UNIQUE())
							{
								CompareUtil.appendIndex(table_develop, index_pdm, index_develop, 13, sb, vSql);
								iCount4++;
							}
							if (index_develop.getConstraint_type() != null)
							{
								if (index_pdm.getConstraint_type() == null)
								{
									CompareUtil.appendIndex(table_develop, index_pdm, index_develop, 14, sb, vSql);
									iCount5++;
								} else if (!index_develop.getConstraint_type().equalsIgnoreCase(
										index_pdm.getConstraint_type()))
								{
									CompareUtil.appendIndex(table_develop, index_pdm, index_develop, 14, sb, vSql);
									iCount5++;
								}
							} else if (index_pdm.getConstraint_type() != null)
							{
								CompareUtil.appendIndex(table_develop, index_pdm, index_develop, 14, sb, vSql);
								iCount5++;
							}
						}
					}
				}
				// add
				for (Iterator iter_column = table_pdm.indexs.keySet().iterator(); iter_column.hasNext();)
				{
					String key_index = (String) iter_column.next();

					// 尝试从PDM库中获得同名索引
					TableIndex index_pdm = (TableIndex) table_pdm.indexs.get(key_index);
					TableIndex index_develop = (TableIndex) table_develop.indexs.get(key_index);
					if (index_develop == null)
					{
						CompareUtil.appendIndex(table_develop, index_pdm, null, 15, sb, vSql);
						iCount1++;
					}
				}
			} else
			{
				// PDM找不到对应的表
				logger.info(table_develop.indexs.size());
				for (Iterator iter_column = table_develop.indexs.keySet().iterator(); iter_column.hasNext();)
				{
					String key_index = (String) iter_column.next();
					if (key_index != null)
					{
						// 获得开发库中的索引
						TableIndex index_develop = (TableIndex) table_develop.indexs.get(key_index);
						CompareUtil.appendIndex(table_develop, null, index_develop, 11, sb, vSql);
						iCount2++;
					}
				}
			}
		}

		// 遍历PDMMap
		for (Iterator iter_table = map_pdm.keySet().iterator(); iter_table.hasNext();)
		{
			String key_table = (String) iter_table.next();
			// 获得开发库中的表
			Table table_develop = map_develop.get(key_table);
			// 尝试从生产库中获得同名表
			Table table_pdm = map_pdm.get(key_table);
			if (table_develop == null)
			{
				for (Iterator iter_column = table_pdm.indexs.keySet().iterator(); iter_column.hasNext();)
				{
					String key_index = (String) iter_column.next();
					// 尝试从PDM库中获得同名索引
					TableIndex index_pdm = (TableIndex) table_pdm.indexs.get(key_index);

					CompareUtil.appendIndex(table_pdm, index_pdm, null, 10, sb, vSql);
					iCount1++;
				}
			}
		}

	}

	public LinkedHashMap<String, Table> GetTableIndexs(Connection transaction, String inDomain, CompareInfo cCompareInfo)
			throws Exception
	{
		logger.info("GetTableIndexs:" + inDomain);
		String sSql = " select * from (select t1.TABLE_NAME,t1.COLUMN_NAME,t1.INDEX_NAME,t1.SEQ_IN_INDEX, t1.NON_UNIQUE,t2.constraint_type"
				+ " from information_schema.STATISTICS t1 left join  information_schema.table_constraints t2 on "
				+ " ( t1.index_name=t2.constraint_name and t1.table_name=t2.table_name and t1.table_schema=t2.table_schema "
				+ " ) where t1.table_schema= ?  "
				+ " union all select TABLE_NAME,COLUMN_NAME,constraint_NAME,1,0,'FOREIGN KEY'  from information_schema.KEY_COLUMN_USAGE "
				+ " where  referenced_table_schema is not null and TABLE_schema=? "
				+ " union all select table_name,null,null,null,null,null from information_schema.tables where table_schema=?"
				+ " and table_name REGEXP '[a-zA-Z]_[0-9]' and table_name not in "
				+ " (select table_name from information_schema.STATISTICS where table_schema=?))t4";

		if (cCompareInfo.getStrExcludeTable() != null && cCompareInfo.getStrExcludeTable().length() > 0)
		{
			sSql +=" where 1=1 ";
			String[] excludeTables=cCompareInfo.getStrExcludeTable().split(",");
			for(int i=0;i<excludeTables.length;i++)
			{
				sSql += " and table_name not like '" + excludeTables[i] + "%' ";
			}
		}
		sSql += " order By table_name,INDEX_NAME,SEQ_IN_INDEX";

		PreparedStatement pstmt = transaction.prepareStatement(sSql);
		pstmt.setQueryTimeout(iSqlTimeOut);
		pstmt.setString(1, inDomain);
		pstmt.setString(2, inDomain);
		pstmt.setString(3, inDomain);
		pstmt.setString(4, inDomain);
		logger.debug(sSql);
		logger.debug("Domain:" + inDomain);

		ResultSet rs = pstmt.executeQuery();

		LinkedHashMap<String, Table> map = new LinkedHashMap<String, Table>();
		String tableName = "";
		Table cTable = null;
		while (rs.next())
		{
			if (!tableName.equals(rs.getString("table_name").toLowerCase().trim()))
			{// 一张新表
				tableName = rs.getString("table_name").toLowerCase().trim();
				cTable = new Table(tableName, "");
				TableIndex cTableIndex = new TableIndex(rs.getString("INDEX_NAME"), rs.getInt("NON_UNIQUE"));
				cTableIndex.AddColName(rs.getString("COLUMN_NAME"));
				cTableIndex.setConstraint_type(rs.getString("constraint_type"));
				cTable.indexs.put(cTableIndex.getIndexName(), cTableIndex);
				map.put(tableName, cTable);
			} else
			{// 已存在的表，增加字段
				TableIndex cTableIndex = (TableIndex) cTable.indexs.get(rs.getString("INDEX_NAME"));
				if (cTableIndex == null)
				{
					TableIndex cTableIndex2 = new TableIndex(rs.getString("INDEX_NAME"), rs.getInt("NON_UNIQUE"));
					cTableIndex2.AddColName(rs.getString("COLUMN_NAME"));
					cTableIndex2.setConstraint_type(rs.getString("constraint_type"));
					cTable.indexs.put(cTableIndex2.getIndexName(), cTableIndex2);
				} else
				{
					cTableIndex.AddColName(rs.getString("COLUMN_NAME"));
					cTableIndex.setConstraint_type(rs.getString("constraint_type"));
					cTable.indexs.put(cTableIndex.getIndexName(), cTableIndex);
				}
			}
		}
		if (null != rs)
			rs.close();

		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jpf.ci.dbs.compare.AbstractDbCompare#GetHtmlName()
	 */
	@Override
	String GetHtmlName()
	{
		// TODO Auto-generated method stub
		return "compare_index.html";
	}

	protected void InsertResult(String strDbUrl)
	{
		Connection conn = null;
		try
		{
			conn = WallsDbConn.GetInstance().GetConn();
			String strSql = "update dbci set diff11=" + iCount1 + ",diff12=" + iCount2 + ",diff13=" + iCount3
					+ ",diff14=" + iCount4 + ",diff15=" + iCount5 + " where dbinfo='" + strDbUrl
					+ "' and diffdate=current_date";
			logger.info(strSql);
			JpfDbUtils.ExecUpdateSql(conn, strSql);
		} catch (Exception ex)
		{
			// TODO: handle exception
			ex.printStackTrace();
		} finally
		{
			JpfDbUtils.DoClear(conn);
		}
	}

	protected String ShowResult()
	{
		return "<tr><td>" + iCount1 + "</td><td>"
				+ iCount2 + "</td><td>"
				+ iCount3 + "</td><td>"
				+ iCount4 + "</td><td>"
				+ iCount5 + "</td></tr>";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jpf.ci.dbs.compare.AbstractDbCompare#GetMailTitle()
	 */
	@Override
	String GetMailTitle()
	{
		// TODO Auto-generated method stub
		return "数据库索引比对带分表结果(自动发出)";
	}
}
