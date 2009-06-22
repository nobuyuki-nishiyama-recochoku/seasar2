/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.extension.jdbc.gen.maven;

import org.seasar.extension.jdbc.gen.dialect.GenDialect;
import org.seasar.extension.jdbc.gen.internal.command.AbstractCommand;
import org.seasar.extension.jdbc.gen.internal.command.DumpDbMetaCommand;

/**
 * データベースのメタデータをダンプするゴールです。
 * 
 * @author hakoda-te-kun
 * @see DumpDbMetaCommand
 * 
 * @goal dump-dbmeta-task
 */
public class DumpDbMetaTaskMojo extends AbstractS2JdbcGenMojo {

	/** コマンド */
	protected DumpDbMetaCommand command = new DumpDbMetaCommand();

	/**
	 * スキーマ名を設定します。
	 * 
	 * @parameter
	 */
	private String schemaName;

	/**
	 * Javaコード生成の対象とするテーブル名の正規表現を設定します。
	 * 
	 * @parameter
	 */
	private String tableNamePattern;

	/**
	 * Javaコード生成の対象としないテーブル名の正規表現を設定します。
	 * 
	 * @parameter
	 */
	private String ignoreTableNamePattern;

	/**
	 * {@link GenDialect}の実装クラス名を設定します。
	 * 
	 * @parameter
	 */
	private String genDialectClassName;

	@Override
	protected AbstractCommand getCommand() {
		return command;
	}

	@Override
	protected void setCommandSpecificParameters() {
		if (schemaName != null)
			command.setSchemaName(schemaName);
		if (tableNamePattern != null)
			command.setTableNamePattern(tableNamePattern);
		if (ignoreTableNamePattern != null)
			command.setIgnoreTableNamePattern(ignoreTableNamePattern);
		if (genDialectClassName != null)
			command.setGenDialectClassName(genDialectClassName);
	}
}