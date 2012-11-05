package com.facebook.presto.operator;

import com.facebook.presto.block.BlockAssertions;
import com.facebook.presto.block.BlockBuilder;
import com.facebook.presto.tuple.FieldOrderedTupleComparator;
import com.facebook.presto.tuple.TupleInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import org.testng.annotations.Test;

import static com.facebook.presto.operator.OperatorAssertions.assertOperatorEqualsUnordered;
import static com.facebook.presto.operator.OperatorAssertions.createOperator;
import static com.facebook.presto.operator.ProjectionFunctions.singleColumn;
import static com.facebook.presto.tuple.TupleInfo.Type.DOUBLE;
import static com.facebook.presto.tuple.TupleInfo.Type.FIXED_INT_64;
import static com.facebook.presto.tuple.TupleInfo.Type.VARIABLE_BINARY;

public class TestTopNOperator
{
    @Test
    public void testSingleFieldKey()
            throws Exception
    {
        Operator source = createOperator(
                new Page(
                        BlockAssertions.createLongsBlock(0, 1, 2),
                        BlockAssertions.createDoublesBlock(0, 0.1, 0.2)
                ),
                new Page(
                        BlockAssertions.createLongsBlock(2, -1, 4),
                        BlockAssertions.createDoublesBlock(2, -0.1, 0.4)
                ),
                new Page(
                        BlockAssertions.createLongsBlock(4, 5, 4, 6),
                        BlockAssertions.createDoublesBlock(4, 0.5, 0.41, 0.6)
                )
        );

        TopNOperator actual = new TopNOperator(
                source, 2, 0, ImmutableList.of(singleColumn(FIXED_INT_64, 0, 0), singleColumn(DOUBLE, 1, 0))
        );

        Operator expected = createOperator(
                new Page(
                        BlockAssertions.createLongsBlock(0, 5, 6),
                        BlockAssertions.createDoublesBlock(0, 0.5, 0.6)
                )
        );
        assertOperatorEqualsUnordered(actual, expected);
    }

    @Test
    public void testMultiFieldKey()
            throws Exception
    {
        TupleInfo tupleInfo = new TupleInfo(VARIABLE_BINARY, FIXED_INT_64);
        Operator source = createOperator(
                new Page(
                        new BlockBuilder(0, tupleInfo)
                                .append("a").append(1)
                                .append("b").append(2)
                                .build()
                ),
                new Page(
                        new BlockBuilder(2, tupleInfo)
                                .append("f").append(3)
                                .append("a").append(4)
                                .build()
                ),
                new Page(
                        new BlockBuilder(4, tupleInfo)
                                .append("d").append(5)
                                .append("d").append(7)
                                .append("e").append(6)
                                .build()
                )
        );

        TopNOperator actual = new TopNOperator(
                source, 3, 0, ImmutableList.of(ProjectionFunctions.concat(singleColumn(VARIABLE_BINARY, 0, 0), singleColumn(FIXED_INT_64, 0, 1)))
        );


        Operator expected = createOperator(
                new Page(
                        new BlockBuilder(0, tupleInfo)
                                .append("d").append(7)
                                .append("e").append(6)
                                .append("f").append(3)
                                .build()
                )
        );
        assertOperatorEqualsUnordered(actual, expected);
    }

    @Test
    public void testReverseOrder()
            throws Exception
    {
        Operator source = createOperator(
                new Page(
                        BlockAssertions.createLongsBlock(0, 1, 2),
                        BlockAssertions.createDoublesBlock(0, 0.1, 0.2)
                ),
                new Page(
                        BlockAssertions.createLongsBlock(2, -1, 4),
                        BlockAssertions.createDoublesBlock(2, -0.1, 0.4)
                ),
                new Page(
                        BlockAssertions.createLongsBlock(4, 5, 4, 6),
                        BlockAssertions.createDoublesBlock(4, 0.5, 0.41, 0.6)
                )
        );

        TopNOperator actual = new TopNOperator(
                source, 2, 0, ImmutableList.of(singleColumn(FIXED_INT_64, 0, 0), singleColumn(DOUBLE, 1, 0)),
                Ordering.from(FieldOrderedTupleComparator.INSTANCE).reverse()
        );

        Operator expected = createOperator(
                new Page(
                        BlockAssertions.createLongsBlock(0, -1, 1),
                        BlockAssertions.createDoublesBlock(0, -0.1, 0.1)
                )
        );
        assertOperatorEqualsUnordered(actual, expected);
    }
}
