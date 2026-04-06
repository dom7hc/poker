import java.util.Arrays;

public class ArrayStats {
    public static int sum(int[] numbers) {
        int total = 0;
        for (int number : numbers) {
            total += number;
        }
        return total;
    }

    public static int max(int[] numbers) {
        int maxValue = Integer.MIN_VALUE;
        for (int number : numbers) {
            if (number > maxValue) {
                maxValue = number;
            }
        }
        return maxValue;
    }

    public static double average(int[] numbers) {
        return (double) sum(numbers) / numbers.length;
    }

    public static void main(String[] args) {
        int[] numbers = {1,2,3,4,5,6,7,10};

        System.out.println("numbers: " + Arrays.toString(numbers));
        System.out.println("Sum: " + sum(numbers));
        System.out.println("Max: " + max(numbers));
        System.out.println("Average: " + average(numbers));
    }
}
