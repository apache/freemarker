package freemarker.ext.beans;

public final class RationalNumber extends Number {
    
    final int divident;
    final int divisor;
    
    public RationalNumber(int divident, int divisor) {
        this.divident = divident;
        this.divisor = divisor;
    }

    @Override
    public int intValue() {
        return divident / divisor;
    }

    @Override
    public long longValue() {
        return divident / (long) divisor;
    }

    @Override
    public float floatValue() {
        return (float) (divident / (double) divisor);
    }

    @Override
    public double doubleValue() {
        return divident / (double) divisor;
    }

    public int getDivident() {
        return divident;
    }

    public int getDivisor() {
        return divisor;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + divident;
        result = prime * result + divisor;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RationalNumber other = (RationalNumber) obj;
        if (divident != other.divident)
            return false;
        if (divisor != other.divisor)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return divident + "/" + divisor;
    }
    
}