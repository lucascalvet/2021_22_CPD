# Problem description and algorithms explanation

In this project it is analyzed the performance of a single core, taking into account the impact of accessing large amounts of data. This analysis is made by running different algorithms for solving the same problem, also in completly different languages, in this case C++ and Python. The problem to be solved is the multiplication of two square matrixes, which are matrixes of same width and height.

Meanwhile, the algorithms chosen to solve this problem were the simple (naive) matrix multiplication, multiline matrix multiplication and block matrix multiplication, where the initial hypoteses were that they should be incrementarly more efficient in terms of memory usage, and therefore have a better perfomance.

## Naive Matrix Multiplication

This algorithm is the simpler aproach to solve the matrix multiplication problem. It is composed by three loops, the two inner loops traverse all the matrixes elements, line by line in the first matrix and column by column in the second one. The outermost loop just fills the resultant matrix with the results.

## Multiline Matrix Multiplication

At a first glance, this algorithm may seem very similar to the previous one, however, the major difference relies in the way that it traverses the matrixes. The previous one had to get the column of the second matrix, which means that in each iteration it obtains the whole matrix line, when it is only going to need a single element. As it is imaginable, this is very memory expensive, as it in each iteration ```[(n-1)/n * 100] %``` of the data loaded to memory will be useless (being n the size of the matrix). For example, if we have a matrix of size 10 by 10, 90% of the data loaded wont be used to the actual calculations.

## Block Matrix Multiplication

Finaly, this last algorithm

# Performance metrics

In order to better analyse the performance of a single core we come up with a group of concise metrics. The main ones

# Results and analysis

# Conclusions