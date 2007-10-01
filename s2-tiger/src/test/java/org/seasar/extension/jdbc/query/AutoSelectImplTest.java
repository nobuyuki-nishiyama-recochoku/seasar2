/*
 * Copyright 2004-2007 the Seasar Foundation and the Others.
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
package org.seasar.extension.jdbc.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import junit.framework.TestCase;

import org.seasar.extension.jdbc.EntityMeta;
import org.seasar.extension.jdbc.JoinMeta;
import org.seasar.extension.jdbc.JoinType;
import org.seasar.extension.jdbc.PropertyMapper;
import org.seasar.extension.jdbc.PropertyMeta;
import org.seasar.extension.jdbc.ResultSetHandler;
import org.seasar.extension.jdbc.SqlLogRegistry;
import org.seasar.extension.jdbc.SqlLogRegistryLocator;
import org.seasar.extension.jdbc.ValueType;
import org.seasar.extension.jdbc.dialect.PostgreDialect;
import org.seasar.extension.jdbc.dialect.StandardDialect;
import org.seasar.extension.jdbc.dto.AaaDto;
import org.seasar.extension.jdbc.entity.Aaa;
import org.seasar.extension.jdbc.entity.Bbb;
import org.seasar.extension.jdbc.entity.Ccc;
import org.seasar.extension.jdbc.entity.Ddd;
import org.seasar.extension.jdbc.exception.BaseJoinNotFoundRuntimeException;
import org.seasar.extension.jdbc.exception.JoinDuplicatedRuntimeException;
import org.seasar.extension.jdbc.exception.NonEntityRuntimeException;
import org.seasar.extension.jdbc.exception.PropertyNotFoundRuntimeException;
import org.seasar.extension.jdbc.handler.BeanAutoResultSetHandler;
import org.seasar.extension.jdbc.handler.BeanListAutoResultSetHandler;
import org.seasar.extension.jdbc.handler.BeanListSupportLimitAutoResultSetHandler;
import org.seasar.extension.jdbc.manager.JdbcManagerImpl;
import org.seasar.extension.jdbc.mapper.AbstractEntityMapper;
import org.seasar.extension.jdbc.mapper.AbstractRelationshipEntityMapper;
import org.seasar.extension.jdbc.mapper.ManyToOneEntityMapperImpl;
import org.seasar.extension.jdbc.mapper.OneToManyEntityMapperImpl;
import org.seasar.extension.jdbc.mapper.OneToOneEntityMapperImpl;
import org.seasar.extension.jdbc.mapper.PropertyMapperImpl;
import org.seasar.extension.jdbc.meta.ColumnMetaFactoryImpl;
import org.seasar.extension.jdbc.meta.EntityMetaFactoryImpl;
import org.seasar.extension.jdbc.meta.PropertyMetaFactoryImpl;
import org.seasar.extension.jdbc.meta.TableMetaFactoryImpl;
import org.seasar.extension.jdbc.types.ValueTypes;
import org.seasar.extension.jta.TransactionManagerImpl;
import org.seasar.framework.convention.impl.PersistenceConventionImpl;
import org.seasar.framework.mock.sql.MockDataSource;
import org.seasar.framework.util.DisposableUtil;

/**
 * @author higa
 * 
 */
public class AutoSelectImplTest extends TestCase {

    private JdbcManagerImpl manager;

    private AutoSelectImpl query;

    @Override
    protected void setUp() throws Exception {
        manager = new JdbcManagerImpl();
        manager.setTransactionManager(new TransactionManagerImpl());
        manager.setDataSource(new MockDataSource());
        manager.setDialect(new StandardDialect());

        PersistenceConventionImpl convention = new PersistenceConventionImpl();
        EntityMetaFactoryImpl emFactory = new EntityMetaFactoryImpl();
        emFactory.setPersistenceConvention(convention);
        TableMetaFactoryImpl tableMetaFactory = new TableMetaFactoryImpl();
        tableMetaFactory.setPersistenceConvention(convention);
        emFactory.setTableMetaFactory(tableMetaFactory);

        PropertyMetaFactoryImpl pFactory = new PropertyMetaFactoryImpl();
        pFactory.setPersistenceConvention(convention);
        ColumnMetaFactoryImpl cmFactory = new ColumnMetaFactoryImpl();
        cmFactory.setPersistenceConvention(convention);
        pFactory.setColumnMetaFactory(cmFactory);
        emFactory.setPropertyMetaFactory(pFactory);
        emFactory.initialize();
        manager.setEntityMetaFactory(emFactory);

        query = new AutoSelectImpl(manager, Aaa.class);
    }

    @Override
    protected void tearDown() throws Exception {
        DisposableUtil.dispose();
        SqlLogRegistry regisry = SqlLogRegistryLocator.getInstance();
        regisry.clear();
        manager = null;
    }

    /**
     * 
     */
    public void testCallerClass() {
        assertSame(query, query.callerClass(getClass()));
        assertEquals(getClass(), query.callerClass);
    }

    /**
     * 
     */
    public void testCallerMethodName() {
        assertSame(query, query.callerMethodName("hoge"));
        assertEquals("hoge", query.callerMethodName);
    }

    /**
     * 
     */
    public void testMaxRows() {
        assertSame(query, query.maxRows(100));
        assertEquals(100, query.maxRows);
    }

    /**
     * 
     */
    public void testFetchSize() {
        assertSame(query, query.fetchSize(100));
        assertEquals(100, query.fetchSize);
    }

    /**
     * 
     */
    public void testQueryTimeout() {
        assertSame(query, query.queryTimeout(100));
        assertEquals(100, query.queryTimeout);
    }

    /**
     * 
     */
    public void testLimit() {
        assertSame(query, query.limit(100));
        assertEquals(100, query.limit);
    }

    /**
     * 
     */
    public void testOffset() {
        assertSame(query, query.offset(100));
        assertEquals(100, query.offset);
    }

    /**
     * 
     */
    public void testJoin() {
        query.join("bbb");
        assertEquals(1, query.getJoinMetaSize());
        JoinMeta joinMeta = query.getJoinMeta(0);
        assertEquals("bbb", joinMeta.getName());
        assertEquals(JoinType.LEFT_OUTER, joinMeta.getJoinType());
        assertTrue(joinMeta.isFetch());
    }

    /**
     * 
     */
    public void testJoin_joinType() {
        query.join("bbb", JoinType.INNER);
        assertEquals(1, query.getJoinMetaSize());
        JoinMeta joinMeta = query.getJoinMeta(0);
        assertEquals("bbb", joinMeta.getName());
        assertEquals(JoinType.INNER, joinMeta.getJoinType());
        assertTrue(joinMeta.isFetch());
    }

    /**
     * 
     */
    public void testJoin_fetch() {
        query.join("bbb", false);
        assertEquals(1, query.getJoinMetaSize());
        JoinMeta joinMeta = query.getJoinMeta(0);
        assertEquals("bbb", joinMeta.getName());
        assertEquals(JoinType.LEFT_OUTER, joinMeta.getJoinType());
        assertFalse(joinMeta.isFetch());
    }

    /**
     * 
     */
    public void testJoin_joinType_fetch() {
        query.join("bbb", JoinType.INNER, false);
        assertEquals(1, query.getJoinMetaSize());
        JoinMeta joinMeta = query.getJoinMeta(0);
        assertEquals("bbb", joinMeta.getName());
        assertEquals(JoinType.INNER, joinMeta.getJoinType());
        assertFalse(joinMeta.isFetch());
    }

    /**
     * 
     */
    public void testCreateTableAlias() {
        assertEquals("_T1", query.createTableAlias());
        assertEquals("_T2", query.createTableAlias());
    }

    /**
     * 
     */
    public void testPrepareTableAlias() {
        String tableAlias = query.prepareTableAlias(null);
        assertEquals("_T1", tableAlias);
        assertSame(tableAlias, query.getTableAlias(null));
    }

    /**
     * 
     */
    public void testPrepareEntityMeta() {
        EntityMeta entityMeta = query.prepareEntityMeta(Aaa.class, null);
        assertEquals("Aaa", entityMeta.getName());
        assertSame(entityMeta, query.getEntityMeta(null));
    }

    /**
     * 
     */
    public void testPrepareEntityMeta_nonEntity() {
        query.prepareCallerClassAndMethodName("getResultList");
        query.baseClass = AaaDto.class;
        try {
            query.prepareEntityMeta(query.baseClass, null);
            fail();
        } catch (NonEntityRuntimeException e) {
            System.out.println(e);
        }
    }

    /**
     * 
     */
    public void testPrepareEntity_selectClause() {
        query.prepareCallerClassAndMethodName("getResultList");
        List<PropertyMapper> propertyMapperList = new ArrayList<PropertyMapper>(
                50);
        List<Integer> idIndexList = new ArrayList<Integer>();
        EntityMeta entityMeta = query.prepareEntityMeta(query.baseClass, null);
        String tableAlias = query.prepareTableAlias(null);
        query.prepareEntity(entityMeta, tableAlias, propertyMapperList,
                idIndexList);
        assertEquals("select _T1.ID, _T1.NAME, _T1.BBB_ID", query.selectClause
                .toSql());
    }

    /**
     * 
     */
    public void testPrepareEntity_valueTypes() {
        query.prepareCallerClassAndMethodName("getResultList");
        List<PropertyMapper> propertyMapperList = new ArrayList<PropertyMapper>(
                50);
        List<Integer> idIndexList = new ArrayList<Integer>();
        EntityMeta entityMeta = query.prepareEntityMeta(query.baseClass, null);
        String tableAlias = query.prepareTableAlias(null);
        query.prepareEntity(entityMeta, tableAlias, propertyMapperList,
                idIndexList);
        ValueType[] valueTypes = query.getValueTypes();
        assertEquals(3, valueTypes.length);
        assertEquals(ValueTypes.INTEGER, valueTypes[0]);
        assertEquals(ValueTypes.STRING, valueTypes[1]);
        assertEquals(ValueTypes.INTEGER, valueTypes[2]);
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareEntity_propertyMappers() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        List<PropertyMapper> propertyMapperList = new ArrayList<PropertyMapper>(
                50);
        List<Integer> idIndexList = new ArrayList<Integer>();
        EntityMeta entityMeta = query.prepareEntityMeta(query.baseClass, null);
        String tableAlias = query.prepareTableAlias(null);
        query.prepareEntity(entityMeta, tableAlias, propertyMapperList,
                idIndexList);
        PropertyMapperImpl[] propertyMappers = query
                .toPropertyMapperArray(propertyMapperList);
        assertEquals(3, propertyMappers.length);
        assertEquals(0, propertyMappers[0].getPropertyIndex());
        assertEquals(Aaa.class.getDeclaredField("id"), propertyMappers[0]
                .getField());
        assertEquals(1, propertyMappers[1].getPropertyIndex());
        assertEquals(Aaa.class.getDeclaredField("name"), propertyMappers[1]
                .getField());
        assertEquals(2, propertyMappers[2].getPropertyIndex());
        assertEquals(Aaa.class.getDeclaredField("bbbId"), propertyMappers[2]
                .getField());
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareEntity_idIndices() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        List<PropertyMapper> propertyMapperList = new ArrayList<PropertyMapper>(
                50);
        List<Integer> idIndexList = new ArrayList<Integer>();
        EntityMeta entityMeta = query.prepareEntityMeta(query.baseClass, null);
        String tableAlias = query.prepareTableAlias(null);
        query.prepareEntity(entityMeta, tableAlias, propertyMapperList,
                idIndexList);
        int[] idIndices = query.toIdIndexArray(idIndexList);
        assertEquals(1, idIndices.length);
        assertEquals(0, idIndices[0]);
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareTarget_entityMapper() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        AbstractEntityMapper entityMapper = query.getEntityMapper(null);
        assertNotNull(entityMapper);
        PropertyMapperImpl[] propertyMappers = (PropertyMapperImpl[]) entityMapper
                .getPropertyMappers();
        assertEquals(3, propertyMappers.length);
        assertEquals(0, propertyMappers[0].getPropertyIndex());
        assertEquals(Aaa.class.getDeclaredField("id"), propertyMappers[0]
                .getField());
        assertEquals(1, propertyMappers[1].getPropertyIndex());
        assertEquals(Aaa.class.getDeclaredField("name"), propertyMappers[1]
                .getField());
        assertEquals(2, propertyMappers[2].getPropertyIndex());
        assertEquals(Aaa.class.getDeclaredField("bbbId"), propertyMappers[2]
                .getField());
        int[] idIndices = entityMapper.getIdIndices();
        assertEquals(1, idIndices.length);
        assertEquals(0, idIndices[0]);
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareTarget_fromClause() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        assertEquals(" from AAA _T1", query.fromClause.toSql());
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareJoin_tableAlias() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("bbb"));
        assertEquals("_T2", query.getTableAlias("bbb"));
    }

    /**
     * @throws Exception
     * 
     */
    public void testSplitBaseAndProperty() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        String[] names = query.splitBaseAndProperty("bbb");
        assertEquals(2, names.length);
        assertNull(names[0]);
        assertEquals("bbb", names[1]);
    }

    /**
     * @throws Exception
     * 
     */
    public void testSplitBaseAndProperty_nest() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        String[] names = query.splitBaseAndProperty("bbb.ccc");
        assertEquals(2, names.length);
        assertEquals("bbb", names[0]);
        assertEquals("ccc", names[1]);
    }

    /**
     * @throws Exception
     * 
     */
    public void testSplitBaseAndProperty_nest2() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        String[] names = query.splitBaseAndProperty("bbb.ccc.ddd");
        assertEquals(2, names.length);
        assertEquals("bbb.ccc", names[0]);
        assertEquals("ddd", names[1]);
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetBaseEntityMeta() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        EntityMeta entityMeta = query.getBaseEntityMeta("bbb", null);
        assertEquals("Aaa", entityMeta.getName());
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetBaseEntityMeta_baseJoinNotFound() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        try {
            query.getBaseEntityMeta("bbb.ccc", "bbb");
            fail();
        } catch (BaseJoinNotFoundRuntimeException e) {
            System.out.println(e);
            assertEquals("Aaa", e.getEntityName());
            assertEquals("bbb.ccc", e.getJoin());
            assertEquals("bbb", e.getBaseJoin());
        }
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetBaseEntityMapper() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        AbstractEntityMapper entityMapper = query.getBaseEntityMapper("bbb",
                null);
        assertEquals(Aaa.class, entityMapper.getEntityClass());
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetBaseEntityMapper_baseJoinNotFound() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        try {
            query.getBaseEntityMapper("bbb.ccc", "bbb");
            fail();
        } catch (BaseJoinNotFoundRuntimeException e) {
            System.out.println(e);
            assertEquals("Aaa", e.getEntityName());
            assertEquals("bbb.ccc", e.getJoin());
            assertEquals("bbb", e.getBaseJoin());
        }
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetPropertyMeta() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        EntityMeta entityMeta = query.getBaseEntityMeta("bbb", null);
        PropertyMeta propertyMeta = query.getPropertyMeta(entityMeta, "bbb",
                "bbb");
        assertEquals("bbb", propertyMeta.getName());
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetPropertyMeta_propertyMetaNotFound() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        EntityMeta entityMeta = query.getBaseEntityMeta("xxx", null);
        try {
            query.getPropertyMeta(entityMeta, "xxx", "xxx");
            fail();
        } catch (PropertyNotFoundRuntimeException e) {
            System.out.println(e);
            assertEquals("Aaa", e.getEntityName());
            assertEquals("xxx", e.getPropertyName());
        }
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetInverseEntityMeta() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        EntityMeta entityMeta = query.getInverseEntityMeta(Bbb.class, "bbb");
        assertEquals("Bbb", entityMeta.getName());
        assertSame(entityMeta, query.getEntityMeta("bbb"));
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetInverseEntityMeta_badEntity() throws Exception {
        query.baseClass = BadAaa.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        try {
            query.getInverseEntityMeta(BadBbb.class, "bbb");
            fail();
        } catch (RuntimeException e) {
            System.out.println(e);
        }
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetInverseEntityMeta_joinDuplicated() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.getInverseEntityMeta(Bbb.class, "bbb");
        try {
            query.getInverseEntityMeta(Bbb.class, "bbb");
            fail();
        } catch (JoinDuplicatedRuntimeException e) {
            System.out.println(e);
        }
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetInversePropertyMeta() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        EntityMeta baseEntityMeta = query.getBaseEntityMeta("bbb", null);
        PropertyMeta relationshipPropertyMeta = query.getPropertyMeta(
                baseEntityMeta, "bbb", "bbb");
        EntityMeta inverseEntityMeta = query.getInverseEntityMeta(Bbb.class,
                "bbb");
        PropertyMeta inversePropertyMeta = query.getInversePropertyMeta(
                inverseEntityMeta, relationshipPropertyMeta);
        assertNotNull(inversePropertyMeta);
        assertEquals("aaa", inversePropertyMeta.getName());
    }

    /**
     * @throws Exception
     * 
     */
    public void testGetInversePropertyMeta_nonOwner() throws Exception {
        query.baseClass = Bbb.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        EntityMeta baseEntityMeta = query.getBaseEntityMeta("aaa", null);
        PropertyMeta relationshipPropertyMeta = query.getPropertyMeta(
                baseEntityMeta, "aaa", "aaa");
        EntityMeta inverseEntityMeta = query.getInverseEntityMeta(Aaa.class,
                "aaa");
        PropertyMeta inversePropertyMeta = query.getInversePropertyMeta(
                inverseEntityMeta, relationshipPropertyMeta);
        assertNotNull(inversePropertyMeta);
        assertEquals("bbb", inversePropertyMeta.getName());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateRelationshipEntityMapper_manyToOne() throws Exception {
        query.baseClass = Ddd.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("bbb"));
        AbstractRelationshipEntityMapper mapper = (AbstractRelationshipEntityMapper) query
                .getEntityMapper("bbb");
        assertNotNull(mapper);
        assertEquals(ManyToOneEntityMapperImpl.class, mapper.getClass());
        assertEquals(Bbb.class, mapper.getEntityClass());
        assertEquals(Ddd.class.getDeclaredField("bbb"), mapper.getField());
        assertEquals(Bbb.class.getDeclaredField("ddds"), mapper
                .getInverseField());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateRelationshipEntityMapper_manyToOne_nullInverse()
            throws Exception {
        query.baseClass = Bbb.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("ccc"));
        AbstractRelationshipEntityMapper mapper = (AbstractRelationshipEntityMapper) query
                .getEntityMapper("ccc");
        assertNotNull(mapper);
        assertEquals(OneToOneEntityMapperImpl.class, mapper.getClass());
        assertEquals(Ccc.class, mapper.getEntityClass());
        assertEquals(Bbb.class.getDeclaredField("ccc"), mapper.getField());
        assertNull(mapper.getInverseField());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateRelationshipEntityMapper_oneToOne() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("bbb"));
        AbstractRelationshipEntityMapper mapper = (AbstractRelationshipEntityMapper) query
                .getEntityMapper("bbb");
        assertNotNull(mapper);
        assertEquals(OneToOneEntityMapperImpl.class, mapper.getClass());
        assertEquals(Bbb.class, mapper.getEntityClass());
        assertEquals(Aaa.class.getDeclaredField("bbb"), mapper.getField());
        assertEquals(Bbb.class.getDeclaredField("aaa"), mapper
                .getInverseField());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateRelationshipEntityMapper_oneToOne_inverse()
            throws Exception {
        query.baseClass = Bbb.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("aaa"));
        AbstractRelationshipEntityMapper mapper = (AbstractRelationshipEntityMapper) query
                .getEntityMapper("aaa");
        assertNotNull(mapper);
        assertEquals(OneToOneEntityMapperImpl.class, mapper.getClass());
        assertEquals(Aaa.class, mapper.getEntityClass());
        assertEquals(Bbb.class.getDeclaredField("aaa"), mapper.getField());
        assertEquals(Aaa.class.getDeclaredField("bbb"), mapper
                .getInverseField());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateRelationshipEntityMapper_oneToMany() throws Exception {
        query.baseClass = Bbb.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("ddds"));
        AbstractRelationshipEntityMapper mapper = (AbstractRelationshipEntityMapper) query
                .getEntityMapper("ddds");
        assertNotNull(mapper);
        assertEquals(OneToManyEntityMapperImpl.class, mapper.getClass());
        assertEquals(Ddd.class, mapper.getEntityClass());
        assertEquals(Bbb.class.getDeclaredField("ddds"), mapper.getField());
        assertEquals(Ddd.class.getDeclaredField("bbb"), mapper
                .getInverseField());
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareJoin_sql() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("bbb"));
        String expected = "select _T1.ID, _T1.NAME, _T1.BBB_ID, _T2.ID, _T2.NAME, _T2.CCC_ID from AAA _T1 left outer join BBB _T2 on _T1.BBB_ID = _T2.ID";
        assertEquals(expected, query.toSql());
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareJoin_sql_mappedBy() throws Exception {
        query.baseClass = Bbb.class;
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("aaa"));
        String expected = "select _T1.ID, _T1.NAME, _T1.CCC_ID, _T2.ID, _T2.NAME, _T2.BBB_ID from BBB _T1 left outer join AAA _T2 on _T2.BBB_ID = _T1.ID";
        assertEquals(expected, query.toSql());
    }

    /**
     * @throws Exception
     * 
     */
    public void testPrepareJoin_sql_nest() throws Exception {
        query.prepareCallerClassAndMethodName("getResultList");
        query.prepareTarget();
        query.prepareJoin(new JoinMeta("bbb"));
        query.prepareJoin(new JoinMeta("bbb.ccc"));
        String expected = "select _T1.ID, _T1.NAME, _T1.BBB_ID, _T2.ID, _T2.NAME, _T2.CCC_ID, _T3.ID, _T3.NAME from AAA _T1 left outer join BBB _T2 on _T1.BBB_ID = _T2.ID left outer join CCC _T3 on _T2.CCC_ID = _T3.ID";
        assertEquals(expected, query.toSql());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateResultListResultSetHandler_nopaging()
            throws Exception {
        query.prepare("getResultList");
        ResultSetHandler handler = query.createResultListResultSetHandler();
        assertEquals(BeanListAutoResultSetHandler.class, handler.getClass());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateResultListResultSetHandler_paging_notSupportsLimit()
            throws Exception {
        query.limit(10);
        query.prepare("getResultList");
        ResultSetHandler handler = query.createResultListResultSetHandler();
        assertEquals(BeanListSupportLimitAutoResultSetHandler.class, handler
                .getClass());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateResultListResultSetHandler_paging_supportsLimit()
            throws Exception {
        manager.setDialect(new PostgreDialect());
        query.limit(10);
        query.prepare("getResultList");
        ResultSetHandler handler = query.createResultListResultSetHandler();
        assertEquals(BeanListAutoResultSetHandler.class, handler.getClass());
    }

    /**
     * @throws Exception
     * 
     */
    public void testCreateSingleResultResultSetHandler() throws Exception {
        query.prepare("getResultList");
        ResultSetHandler handler = query.createSingleResultResultSetHandler();
        assertEquals(BeanAutoResultSetHandler.class, handler.getClass());
    }

    /**
     * @throws Exception
     * 
     */
    public void testWhere() throws Exception {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("aaa", 1);
        assertSame(query, query.where(m));
        assertSame(m, query.conditions);
    }

    /**
     * @throws Exception
     * 
     */
    public void testAddCondition() throws Exception {
        query.prepare("getResultList");
        query.addCondition("id = ?", 1, Integer.class);
        assertEquals(" where id = ?", query.whereClause.toSql());
        assertEquals(1, query.bindVariableList.size());
        assertEquals(1, query.bindVariableList.get(0));
        assertEquals(1, query.bindVariableClassList.size());
        assertEquals(Integer.class, query.bindVariableClassList.get(0));
    }

    /**
     * 
     */
    public void testAddInCondition() {
        query.prepare("getResultList");
        int[] vars = new int[] { 1, 2 };
        query.addInCondition("_T1.AAA_ID in (?, ?)", vars, int.class, 2);
        assertEquals(" where _T1.AAA_ID in (?, ?)", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(2, variables.length);
        assertEquals(1, variables[0]);
        assertEquals(2, variables[1]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(2, variableClasses.length);
        assertEquals(int.class, variableClasses[0]);
        assertEquals(int.class, variableClasses[1]);
    }

    /**
     * 
     */
    public void testPrepareCondition_EQ() {
        query.prepare("getResultList");
        query.prepareCondition("id", 1);
        assertEquals(" where _T1.ID = ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_EQ2() {
        query.prepare("getResultList");
        query.prepareCondition("id_EQ", 1);
        assertEquals(" where _T1.ID = ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_EQ_NEST() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id", 1);
        assertEquals(" where _T2.ID = ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_NE() {
        query.prepare("getResultList");
        query.prepareCondition("id_NE", 1);
        assertEquals(" where _T1.ID <> ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_NE_NEST() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_NE", 1);
        assertEquals(" where _T2.ID <> ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_LT() {
        query.prepare("getResultList");
        query.prepareCondition("id_LT", 1);
        assertEquals(" where _T1.ID < ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_LT_NEST() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_LT", 1);
        assertEquals(" where _T2.ID < ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_LE() {
        query.prepare("getResultList");
        query.prepareCondition("id_LE", 1);
        assertEquals(" where _T1.ID <= ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_LE_NEST() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_LE", 1);
        assertEquals(" where _T2.ID <= ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_GT() {
        query.prepare("getResultList");
        query.prepareCondition("id_GT", 1);
        assertEquals(" where _T1.ID > ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_GT_NEST() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_GT", 1);
        assertEquals(" where _T2.ID > ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_GE() {
        query.prepare("getResultList");
        query.prepareCondition("id_GE", 1);
        assertEquals(" where _T1.ID >= ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_GE_NEST() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_GE", 1);
        assertEquals(" where _T2.ID >= ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals(1, variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_IN() {
        query.prepare("getResultList");
        query.prepareCondition("id_IN", Arrays.asList(1, 2));
        assertEquals(" where _T1.ID in (?, ?)", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(2, variables.length);
        assertEquals(1, variables[0]);
        assertEquals(2, variables[1]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(2, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
        assertEquals(Integer.class, variableClasses[1]);
    }

    /**
     * 
     */
    public void testPrepareCondition_IN_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_IN", Arrays.asList(1, 2));
        assertEquals(" where _T2.ID in (?, ?)", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(2, variables.length);
        assertEquals(1, variables[0]);
        assertEquals(2, variables[1]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(2, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
        assertEquals(Integer.class, variableClasses[1]);
    }

    /**
     * 
     */
    public void testPrepareCondition_NOT_IN() {
        query.prepare("getResultList");
        query.prepareCondition("id_NOT_IN", Arrays.asList(1, 2));
        assertEquals(" where _T1.ID not in (?, ?)", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(2, variables.length);
        assertEquals(1, variables[0]);
        assertEquals(2, variables[1]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(2, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
        assertEquals(Integer.class, variableClasses[1]);
    }

    /**
     * 
     */
    public void testPrepareCondition_NOT_IN_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.id_NOT_IN", Arrays.asList(1, 2));
        assertEquals(" where _T2.ID not in (?, ?)", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(2, variables.length);
        assertEquals(1, variables[0]);
        assertEquals(2, variables[1]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(2, variableClasses.length);
        assertEquals(Integer.class, variableClasses[0]);
        assertEquals(Integer.class, variableClasses[1]);
    }

    /**
     * 
     */
    public void testPrepareCondition_LIKE() {
        query.prepare("getResultList");
        query.prepareCondition("name_LIKE", "aaa");
        assertEquals(" where _T1.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("aaa", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_LIKE_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.name_LIKE", "aaa");
        assertEquals(" where _T2.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("aaa", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_STARTS() {
        query.prepare("getResultList");
        query.prepareCondition("name_STARTS", "aaa");
        assertEquals(" where _T1.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("aaa%", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_STARTS_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.name_STARTS", "aaa");
        assertEquals(" where _T2.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("aaa%", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_ENDS() {
        query.prepare("getResultList");
        query.prepareCondition("name_ENDS", "aaa");
        assertEquals(" where _T1.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("%aaa", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_ENDS_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.name_ENDS", "aaa");
        assertEquals(" where _T2.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("%aaa", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_CONTAINS() {
        query.prepare("getResultList");
        query.prepareCondition("name_CONTAINS", "aaa");
        assertEquals(" where _T1.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("%aaa%", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_CONTAINS_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.name_CONTAINS", "aaa");
        assertEquals(" where _T2.NAME like ?", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(1, variables.length);
        assertEquals("%aaa%", variables[0]);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(1, variableClasses.length);
        assertEquals(String.class, variableClasses[0]);
    }

    /**
     * 
     */
    public void testPrepareCondition_IS_NULL() {
        query.prepare("getResultList");
        query.prepareCondition("name_IS_NULL", true);
        assertEquals(" where _T1.NAME is null", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(0, variables.length);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(0, variableClasses.length);
    }

    /**
     * 
     */
    public void testPrepareCondition_IS_NULL_false() {
        query.prepare("getResultList");
        query.prepareCondition("name_IS_NULL", false);
        assertEquals("", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(0, variables.length);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(0, variableClasses.length);
    }

    /**
     * 
     */
    public void testPrepareCondition_IS_NULL_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.name_IS_NULL", true);
        assertEquals(" where _T2.NAME is null", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(0, variables.length);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(0, variableClasses.length);
    }

    /**
     * 
     */
    public void testPrepareCondition_IS_NOT_NULL() {
        query.prepare("getResultList");
        query.prepareCondition("name_IS_NOT_NULL", true);
        assertEquals(" where _T1.NAME is not null", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(0, variables.length);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(0, variableClasses.length);
    }

    /**
     * 
     */
    public void testPrepareCondition_IS_NOT_NULL_false() {
        query.prepare("getResultList");
        query.prepareCondition("name_IS_NOT_NULL", false);
        assertEquals("", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(0, variables.length);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(0, variableClasses.length);
    }

    /**
     * 
     */
    public void testPrepareCondition_IS_NOT_NULL_nest() {
        query.join("bbb");
        query.prepare("getResultList");
        query.prepareCondition("bbb.name_IS_NOT_NULL", true);
        assertEquals(" where _T2.NAME is not null", query.whereClause.toSql());
        Object[] variables = query.getBindVariables();
        assertEquals(0, variables.length);
        Class<?>[] variableClasses = query.getBindVariableClasses();
        assertEquals(0, variableClasses.length);
    }

    @Entity
    private static class BadAaa {

        /**
         * 
         */
        @Id
        public Integer id;

        /**
         * 
         */
        public Integer bbbId;

        /**
         * 
         */
        @OneToOne
        public BadBbb bbb;
    }

    @Entity(name = "BadBbb")
    private static class BadBbb {

        /**
         * 
         */
        @Id
        public Integer id;

        /**
         * 
         */
        @OneToOne
        public BadCcc ccc;
    }

    @Entity
    private static class BadCcc {
    }
}