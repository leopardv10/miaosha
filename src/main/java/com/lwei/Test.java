package com.lwei;

import java.util.*;


public class Test {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		int n = sc.nextInt();
		int[] arr = new int[n];

		for(int i = 0; i < n; i++) {
			arr[i] = sc.nextInt();
		}

		if(n % 2 == 0) {
			int t = n / 2;
			int[] one = new int[t];
			int[] two = new int[t];

			for(int i = 0; i < n; i++) {
				if(i < t) {
					one[i] = arr[i];
				} else {
					two[i - t] = arr[i];
				}
			}

			int count = 0;
			for(int i = 0; i < t; i++) {
				if(one[i] != two[i]) {
					count += 1;
				}
			}
			System.out.println(count);
		} else {
			int t = n / 2;
			int[] one = new int[t];
			int[] two = new int[t];

			for(int i = 0; i < n; i++) {
				if(i < t) {
					one[i] = arr[i];
				} else if(i > t) {
					two[i - t - 1] = arr[i];
				}
			}

			int count = 0;
			for(int i = 0; i < t; i++) {
				if(one[i] != two[i]) {
					count += 1;
				}
			}
			System.out.println(count);
		}
	}

	public static int solve(int n, int k, int[] arr) {
		Arrays.sort(arr);
		if(k == 0) {
			if(arr[0] > 1) {
				return arr[0] - 1;
			} else {
				return -1;
			}
		}

		if(k == n) {
			if(arr[n - 1] < n) {
				return arr[n - 1] + 1;
			} else {
				return -1;
			}
		}

		for(int i = 0; i < arr.length; i++) {
			if(i == k - 1) {
				if(arr[i + 1] == arr[i]) {
					return -1;
				} else {
					return arr[i] + 1;
				}
			}
		}
		return -1;
	}
}

