/* This file is part of VoltDB.
 * Copyright (C) 2008-2019 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb.plannerv2;

import org.voltdb.plannerv2.rules.PlannerRules;

public class TestInlineRules extends Plannerv2TestCase {

    private InlineRulesTester m_tester = new InlineRulesTester();

    @Override
    protected void setUp() throws Exception {
        setupSchema(TestValidation.class.getResource(
                "testcalcite-ddl.sql"), "testcalcite", false);
        init();
        m_tester.phase(PlannerRules.Phase.INLINE);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSimpleSeqScan() {
        m_tester.sql("select si from Ri1")
                .transform("VoltSeqTableScan(table=[[public, RI1]], split=[1], expr#0..3=[{inputs}], SI=[$t1])\n")
                .pass();
    }

    public void testSeqScan() {
        m_tester.sql("select * from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}])\n")
                .pass();
    }

    public void testSeqScanWithProjection() {
        m_tester.sql("select i, si from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], proj#0..1=[{exprs}])\n")
                .pass();
    }

    public void testSeqScanWithProjectionExpr() {
        m_tester.sql("select i * 5 from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[5], expr#7=[*($t0, $t6)], EXPR$0=[$t7])\n")
                .pass();
    }

    public void testSeqScanWithFilter() {
        m_tester.sql("select i from R1 where i = 5")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[5], expr#7=[=($t0, $t6)], I=[$t0], $condition=[$t7])\n")
                .pass();
    }

    public void testSeqScanWithFilterParam() {
        m_tester.sql("select i from R1 where i = ? and v = ?")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[?0], expr#7=[=($t0, $t6)], expr#8=[?1], expr#9=[=($t5, $t8)], expr#10=[AND($t7, $t9)], I=[$t0], $condition=[$t10])\n")
                .pass();
    }

    public void testSeqScanWithStringFilter() {
        m_tester.sql("select i from R1 where v = 'FOO1'")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=['FOO1'], expr#7=[=($t5, $t6)], I=[$t0], $condition=[$t7])\n")
                .pass();
    }

    public void testSeqScanWithFilterWithTypeConversion() {
        m_tester.sql("select i from R1 where si = 5")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[CAST($t1):INTEGER], expr#7=[5], expr#8=[=($t6, $t7)], I=[$t0], $condition=[$t8])\n")
                .pass();
    }

    public void testSeqScanWithLimit() {
        m_tester.sql("select i from R1 limit 5")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], I=[$t0], limit=[5])\n")
                .pass();
    }

    public void testSeqScanWithLimitParam() {
        m_tester.sql("select i from R1 limit ?")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], I=[$t0], limit=[?0])\n")
                .pass();
    }

    public void testSeqScanWithFilterAndLimit() {
        m_tester.sql("select i from R1 where si > 3 limit 5")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[3], expr#7=[>($t1, $t6)], I=[$t0], $condition=[$t7], limit=[5])\n")
                .pass();
    }

    public void testSeqScanWithOrderByAndLimitAndFilter() {
        m_tester.sql("select si, i from RI1 where I > 3 order by si limit 3")
                .transform("VoltPhysicalSort(sort0=[$0], dir0=[ASC], fetch=[3], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, RI1]], split=[1], expr#0..3=[{inputs}], expr#4=[3], expr#5=[>($t0, $t4)], SI=[$t1], I=[$t0], $condition=[$t5])\n")
                .pass();
    }

    public void testSeqScanWithOffset() {
        m_tester.sql("select i from R1 offset 1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], I=[$t0], offset=[1])\n")
                .pass();
    }

    public void testSeqScanWithLimitOffset() {
        m_tester.sql("select i from R1 limit 5 offset 1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], I=[$t0], limit=[5], offset=[1])\n")
                .pass();
    }

    public void testSeqScanWithLimitOffsetSort() {
        m_tester.sql("select i from R1 order by bi limit 5 offset 1")
                .transform("VoltPhysicalSort(sort0=[$1], dir0=[ASC], offset=[1], fetch=[5], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], I=[$t0], BI=[$t3])\n")
                .pass();
    }

    public void testSeqScanWithOrderByAndFilter() {
        m_tester.sql("select * from R1 where si > 3 order by i")
                .transform("VoltPhysicalSort(sort0=[$0], dir0=[ASC], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[3], expr#7=[>($t1, $t6)], proj#0..5=[{exprs}], $condition=[$t7])\n")
                .pass();

        m_tester.sql("select i, bi, si from R1 where si > 3 order by i")
                .transform("VoltPhysicalSort(sort0=[$0], dir0=[ASC], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[3], expr#7=[>($t1, $t6)], I=[$t0], BI=[$t3], SI=[$t1], $condition=[$t7])\n")
                .pass();
    }

    public void testSeqScanWithOrderBy() {
        m_tester.sql("select si from R1 order by i, si desc")
                .transform("VoltPhysicalSort(sort0=[$1], sort1=[$0], dir0=[ASC], dir1=[DESC], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], SI=[$t1], I=[$t0])\n")
                .pass();

        m_tester.sql("select bi, i, si from R1 order by i, si desc")
                .transform("VoltPhysicalSort(sort0=[$1], sort1=[$2], dir0=[ASC], dir1=[DESC], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], BI=[$t3], I=[$t0], SI=[$t1])\n")
                .pass();
    }

    public void testSeqScanWithOrderByExpr() {
        m_tester.sql("select bi, i, si from R1 order by i, si + 1 desc")
                .transform("VoltPhysicalSort(sort0=[$1], sort1=[$3], dir0=[ASC], dir1=[DESC], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[1], expr#7=[+($t1, $t6)], BI=[$t3], I=[$t0], SI=[$t1], EXPR$3=[$t7])\n")
                .pass();
    }

    public void testSeqScanWithOrderByAndLimit() {
        m_tester.sql("select bi, i, si from R1 order by i limit 5")
                .transform("VoltPhysicalSort(sort0=[$1], dir0=[ASC], fetch=[5], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], BI=[$t3], I=[$t0], SI=[$t1])\n")
                .pass();
    }

    public void testIndexScanWithLimitOffset() {
        m_tester.sql("select si, i from RI1 where I > 3 limit 3 offset 4")
                .transform("VoltSeqTableScan(table=[[public, RI1]], split=[1], expr#0..3=[{inputs}], expr#4=[3], expr#5=[>($t0, $t4)], SI=[$t1], I=[$t0], $condition=[$t5], limit=[3], offset=[4])\n")
                .pass();
    }

    public void testConstIntExpr() {
        m_tester.sql("select 5 from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[5], EXPR$0=[$t6])\n")
                .pass();
    }

    public void testBinaryIntExpr() {
        m_tester.sql("select 5 + i from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[5], expr#7=[+($t6, $t0)], EXPR$0=[$t7])\n")
                .pass();
    }

    public void testConstBinaryIntExpr() {
        m_tester.sql("select 5 + 5 from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[5], expr#7=[+($t6, $t6)], EXPR$0=[$t7])\n")
                .pass();
    }

    public void testConcatStringExpr() {
        m_tester.sql("select '55' || '22' from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=['55'], expr#7=['22'], expr#8=[||($t6, $t7)], EXPR$0=[$t8])\n")
                .pass();

        m_tester.sql("select v || '22' from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=['22'], expr#7=[||($t5, $t6)], EXPR$0=[$t7])\n")
                .pass();
    }

    public void testDatetimeConstExpr() {
        m_tester.sql("select TIMESTAMP '1969-07-20 20:17:40' from RTYPES")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[1969-07-20 20:17:40], EXPR$0=[$t9])\n")
                .pass();
    }

    public void testBinaryDatetimeExpr() {
        m_tester.sql("select ts - INTERVAL '1' DAY from RTYPES")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[86400000], expr#10=[-($t6, $t9)], EXPR$0=[$t10])\n")
                .pass();
    }

    public void testCompareInExpr() {
        // Calcite transforms the IN expression into ORs
        m_tester.sql("select 1 from RTYPES where i IN (1, 2)")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[1], expr#10=[=($t3, $t9)], expr#11=[2], expr#12=[=($t3, $t11)], expr#13=[OR($t10, $t12)], EXPR$0=[$t9], $condition=[$t13])\n")
                .pass();

        m_tester.sql("select 1 from RTYPES where i IN (1, ?, 3)")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[1], expr#10=[=($t3, $t9)], expr#11=[?0], expr#12=[=($t3, $t11)], expr#13=[3], expr#14=[=($t3, $t13)], expr#15=[OR($t10, $t12, $t14)], EXPR$0=[$t9], $condition=[$t15])\n")
                .pass();
    }

    public void testCompareLikeExpr() {
        m_tester.sql("select 1 from RTYPES where vc LIKE 'ab%c'")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[1], expr#10=['ab%c'], expr#11=[LIKE($t8, $t10)], EXPR$0=[$t9], $condition=[$t11])\n")
                .pass();

        m_tester.sql("select 1 from RTYPES where vc LIKE ?")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[1], expr#10=[?0], expr#11=[LIKE($t8, $t10)], EXPR$0=[$t9], $condition=[$t11])\n")
                .pass();
    }

    public void testAbsExpr() {
        m_tester.sql("select abs(i) from RTYPES")
                .transform("VoltSeqTableScan(table=[[public, RTYPES]], split=[1], expr#0..8=[{inputs}], expr#9=[ABS($t3)], EXPR$0=[$t9])\n")
                .pass();
    }

    public void testAggr() {
        m_tester.sql("select avg(ti) from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0=[{inputs}], EXPR$0=[$t0], aggregate=[VoltPhysicalSerialAggregate.CONVENTION.[].single(input=HepRelVertex#49,group={},EXPR$0=AVG($0),split=1,coordinator=false,type=serial)_split_1_coordinator_false])\n")
                .pass();

        m_tester.sql("select avg(ti) from R1 group by i")
                .transform("VoltPhysicalCalc(expr#0..1=[{inputs}], EXPR$0=[$t1], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..1=[{inputs}], proj#0..1=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#128,group={0},EXPR$0=AVG($1),split=1,coordinator=false,type=hash)_split_1_coordinator_false])\n")
                .pass();

        m_tester.sql("select count(i) from R1 where ti > 3")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0=[{inputs}], EXPR$0=[$t0], aggregate=[VoltPhysicalSerialAggregate.CONVENTION.[].single(input=HepRelVertex#196,group={},EXPR$0=COUNT($0),split=1,coordinator=false,type=serial)_split_1_coordinator_false])\n")
                .pass();

        m_tester.sql("select count(*) from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0=[{inputs}], EXPR$0=[$t0], aggregate=[VoltPhysicalSerialAggregate.CONVENTION.[].single(input=HepRelVertex#252,group={},EXPR$0=COUNT(),split=1,coordinator=false,type=serial)_split_1_coordinator_false])\n")
                .pass();

        m_tester.sql("select max(TI) from R1 group by SI having SI > 0")
                .transform("VoltPhysicalCalc(expr#0..1=[{inputs}], EXPR$0=[$t1], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..1=[{inputs}], proj#0..1=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#341,group={0},EXPR$0=MAX($1),split=1,coordinator=false,having=>($0, 0),type=hash)_split_1_coordinator_false>($0, 0)])\n")
                .pass();

        m_tester.sql("select max(TI), SI, min(TI), I from R1 group by SI, I having avg(BI) > max(BI)")
                .transform("VoltPhysicalCalc(expr#0..5=[{inputs}], EXPR$0=[$t2], SI=[$t0], EXPR$2=[$t3], I=[$t1], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#437,group={0, 1},EXPR$0=MAX($2),EXPR$2=MIN($2),agg#2=AVG($3),agg#3=MAX($3),split=1,coordinator=false,having=>($4, $5),type=hash)_split_1_coordinator_false>($4, $5)])\n")
                .pass();

        m_tester.sql("select max(TI), SI, I, min(TI) from R1 group by I, SI having avg(BI) > 0 and si > 0")
                .transform("VoltPhysicalCalc(expr#0..4=[{inputs}], EXPR$0=[$t2], SI=[$t1], I=[$t0], EXPR$3=[$t3], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..4=[{inputs}], proj#0..4=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#533,group={0, 1},EXPR$0=MAX($2),EXPR$3=MIN($2),agg#2=AVG($3),split=1,coordinator=false,having=AND(>($4, 0), >($1, 0)),type=hash)_split_1_coordinator_falseAND(>($4, 0), >($1, 0))])\n")
                .pass();

        m_tester.sql("select max(TI), SI from R1 where I > 0 group by SI, I order by SI limit 3")
                .transform("VoltPhysicalSort(sort0=[$1], dir0=[ASC], fetch=[3], split=[1])\n" +
                        "  VoltPhysicalCalc(expr#0..2=[{inputs}], EXPR$0=[$t2], SI=[$t0], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..2=[{inputs}], proj#0..2=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#661,group={0, 1},EXPR$0=MAX($2),split=1,coordinator=false,type=hash)_split_1_coordinator_false])\n")
                .pass();
    }

    public void testDistinct() {
        m_tester.sql("select distinct TI, I from R1")
                .transform("VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..1=[{inputs}], proj#0..1=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#54,group={0, 1},split=1,coordinator=false,type=hash)_split_1_coordinator_false])\n")
                .pass();

        m_tester.sql("select distinct max(TI) from R1 group by I")
                .transform("VoltPhysicalHashAggregate(group=[{0}], split=[1], coordinator=[false], type=[hash])\n" +
                        "  VoltPhysicalCalc(expr#0..1=[{inputs}], EXPR$0=[$t1], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..1=[{inputs}], proj#0..1=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#155,group={0},EXPR$0=MAX($1),split=1,coordinator=false,type=hash)_split_1_coordinator_false])\n")
                .pass();

        m_tester.sql("select max (distinct (TI)) from R1 group by I")
                .transform("VoltPhysicalCalc(expr#0..1=[{inputs}], EXPR$0=[$t1], split=[1])\n" +
                        "  VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..1=[{inputs}], proj#0..1=[{exprs}], aggregate=[VoltPhysicalHashAggregate.CONVENTION.[].single(input=HepRelVertex#238,group={0},EXPR$0=MAX(DISTINCT $1),split=1,coordinator=false,type=hash)_split_1_coordinator_false])\n")
                .pass();
    }

    public void testJoin() {
        m_tester.sql("select R1.i, R2.v from R1, R2 " +
                "where R2.si = R1.i and R2.v = 'foo'")
                .transform("VoltPhysicalCalc(expr#0..11=[{inputs}], I=[$t0], V=[$t11], split=[1])\n" +
                        "  VoltPhysicalJoin(condition=[=(CAST($7):INTEGER, $0)], joinType=[inner], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}])\n" +
                        "    VoltSeqTableScan(table=[[public, R2]], split=[1], expr#0..5=[{inputs}], expr#6=['foo'], expr#7=[=($t5, $t6)], proj#0..5=[{exprs}], $condition=[$t7])\n")
                .pass();

        m_tester.sql("select R1.i, R2.v from R1 inner join R2 " +
                "on R2.si = R1.i where R2.v = 'foo'")
                .transform("VoltPhysicalCalc(expr#0..12=[{inputs}], I=[$t0], V=[$t11], split=[1])\n" +
                        "  VoltPhysicalJoin(condition=[=($12, $0)], joinType=[inner], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}])\n" +
                        "    VoltSeqTableScan(table=[[public, R2]], split=[1], expr#0..5=[{inputs}], expr#6=[CAST($t1):INTEGER], expr#7=['foo'], expr#8=[=($t5, $t7)], proj#0..6=[{exprs}], $condition=[$t8])\n")
                .pass();

        m_tester.sql("select R2.si, R1.i from R1 inner join " +
                "R2 on R2.i = R1.si where R2.v = 'foo' and R1.si > 4 and R1.ti > R2.i")
                .transform("VoltPhysicalCalc(expr#0..12=[{inputs}], SI=[$t8], I=[$t0], split=[1])\n" +
                        "  VoltPhysicalJoin(condition=[AND(=($7, $6), >($2, $7))], joinType=[inner], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[CAST($t1):INTEGER], expr#7=[4], expr#8=[>($t1, $t7)], proj#0..6=[{exprs}], $condition=[$t8])\n" +
                        "    VoltSeqTableScan(table=[[public, R2]], split=[1], expr#0..5=[{inputs}], expr#6=['foo'], expr#7=[=($t5, $t6)], proj#0..5=[{exprs}], $condition=[$t7])\n")
                .pass();

        m_tester.sql("select R1.i from R1 inner join " +
                "R2  on R1.si = R2.si where R1.I + R2.ti = 5")
                .transform("VoltPhysicalCalc(expr#0..11=[{inputs}], I=[$t0], split=[1])\n" +
                        "  VoltPhysicalJoin(condition=[AND(=($1, $7), =(+($0, $8), 5))], joinType=[inner], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}])\n" +
                        "    VoltSeqTableScan(table=[[public, R2]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}])\n")
                .pass();
    }

    public void testThreeWayJoin() {
        m_tester.sql("select R1.i from R1 inner join " +
                "R2  on R1.si = R2.i inner join " +
                "R3 on R2.v = R3.vc where R1.si > 4 and R3.vc <> 'foo'")
                .transform("VoltPhysicalCalc(expr#0..15=[{inputs}], I=[$t0], split=[1])\n" +
                        "  VoltPhysicalJoin(condition=[=($12, $14)], joinType=[inner], split=[1])\n" +
                        "    VoltPhysicalCalc(expr#0..12=[{inputs}], expr#13=[CAST($t12):VARCHAR(256) CHARACTER SET \"ISO-8859-1\" COLLATE \"ISO-8859-1$en_US$primary\"], proj#0..5=[{exprs}], I0=[$t7], SI1=[$t8], TI0=[$t9], BI0=[$t10], F0=[$t11], V0=[$t12], V00=[$t13], split=[1])\n" +
                        "      VoltPhysicalJoin(condition=[=($6, $7)], joinType=[inner], split=[1])\n" +
                        "        VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=[CAST($t1):INTEGER], expr#7=[4], expr#8=[>($t1, $t7)], proj#0..6=[{exprs}], $condition=[$t8])\n" +
                        "        VoltSeqTableScan(table=[[public, R2]], split=[1], expr#0..5=[{inputs}], proj#0..5=[{exprs}])\n" +
                        "    VoltSeqTableScan(table=[[public, R3]], split=[1], expr#0..2=[{inputs}], expr#3=['foo'], expr#4=[<>($t1, $t3)], proj#0..2=[{exprs}], $condition=[$t4])\n")
                .pass();
    }

    public void testSubqueriesJoin() {
        m_tester.sql("select t1.v, t2.v "
                + "from "
                + "  (select * from R1 where v = 'foo') as t1 "
                + "  inner join "
                + "  (select * from R2 where f = 30.3) as t2 "
                + "on t1.i = t2.i "
                + "where t1.i = 3")
                .transform("VoltPhysicalCalc(expr#0..11=[{inputs}], V=[$t5], V0=[$t11], split=[1])\n" +
                        "  VoltPhysicalJoin(condition=[=($0, $6)], joinType=[inner], split=[1])\n" +
                        "    VoltSeqTableScan(table=[[public, R1]], split=[1], expr#0..5=[{inputs}], expr#6=['foo'], expr#7=[=($t5, $t6)], expr#8=[3], expr#9=[=($t0, $t8)], expr#10=[AND($t7, $t9)], proj#0..5=[{exprs}], $condition=[$t10])\n" +
                        "    VoltSeqTableScan(table=[[public, R2]], split=[1], expr#0..5=[{inputs}], expr#6=[CAST($t4):DOUBLE NOT NULL], expr#7=[30.3], expr#8=[=($t6, $t7)], proj#0..5=[{exprs}], $condition=[$t8])\n")
                .pass();
    }
}