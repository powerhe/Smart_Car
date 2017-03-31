package com.lenovo.newdevice.tangocar.path.finder.data;

import android.graphics.Point;

/**
 * A point comprised of a x and a column with an associated cost comprised of a to cost and a from cost for use in an
 * A* search algorithm
 */
public class WeightedPoint extends Point implements Comparable<WeightedPoint> {

    /**
     * The cost from a starting point to have reached this point (g)
     */
    private float fromCost;

    /**
     * The estimated cost to go from this point to a goal (h)
     */
    private float toCost;

    /**
     * A link to the previous point in a linked list of points tracing the path from this point back to the start
     */
    private WeightedPoint prev;

    /**
     * Construct a weighted point without assigning a from cost
     *
     * @param row
     * @param col
     */
    public WeightedPoint(int row, int col) {
        this(row, col, 0);
    }

    /**
     * Construct a weighted point
     *
     * @param row
     * @param col
     * @param fromCost
     */
    public WeightedPoint(int row, int col, int fromCost) {
        this.x = row;
        this.y = col;
        this.fromCost = fromCost;
    }

    /**
     * Returns a String starting with the specified label and the weighted point toString method in a null-safe manner
     *
     * @param label
     * @param wp
     * @return labeledString
     */
    public static String toLabeledString(String label, WeightedPoint wp) {
        return label + ": " + (wp == null ? "" : wp.toString());
    }

    /**
     * Get the x for this point
     *
     * @return x
     */
    public int getX() {
        return x;
    }

    /**
     * Set the x for this point
     *
     * @param x
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the column for this point
     *
     * @return y
     */
    public int getY() {
        return y;
    }

    /**
     * Set the column for this point
     *
     * @param y
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Get the total cost (f)<br />
     * <i>fromCost + toCost</i><br />
     * (f=g+h)
     *
     * @return totalCost
     */
    public float getCost() {
        return fromCost + toCost;
    }

    /**
     * Get the from cost (g)
     *
     * @return fromCost
     */
    public float getFromCost() {
        return fromCost;
    }

    /**
     * Set the from cost (g)
     *
     * @param fromCost
     */
    public void setFromCost(float fromCost) {
        this.fromCost = fromCost;
    }

    /**
     * Get the to cost (h)
     *
     * @return toCost
     */
    public float getToCost() {
        return toCost;
    }

    /**
     * Set the to cost (h)
     *
     * @param toCost
     */
    public void setToCost(float toCost) {
        this.toCost = toCost;
    }

    /**
     * Get the link to the previous point in a linked list of points tracing the path from this point back to the start
     *
     * @return prev
     */
    public WeightedPoint getPrev() {
        return prev;
    }

    /**
     * Set the link to the previous point in a linked list of points tracing the path from this point back to the start
     *
     * @param prev
     */
    public void setPrev(WeightedPoint prev) {
        this.prev = prev;
    }

    @Override
    public int compareTo(WeightedPoint other) {
        if (other == null || this.getCost() > other.getCost())
            return 1;
        else if (this.getCost() < other.getCost())
            return -1;
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + y;
        result = prime * result + x;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        else if (other instanceof WeightedPoint) {
            WeightedPoint otherWp = (WeightedPoint) other;
            return (otherWp.x == x && otherWp.y == y);
        }
        return false;
    }

    public String toString() {
        return "(x" + x + ", y" + y + ")";
    }
}
