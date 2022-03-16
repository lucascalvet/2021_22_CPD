import time

def printMatrix(dim, c):
    for i in range(min(10, dim)):
        print(str(c[i]), end = " ")
    print("\n")

def OnMult(dim):
    a = [1.0] * dim ** 2
    b = [i + 1.0 for i in range(dim) for j in range(dim)]
    c = [0.0] * dim ** 2
    
    time1 = time.time()

    for i in range(dim):
        for j in range(dim):
            temp = 0
            for k in range(dim):
                temp += a[i * dim + k] * b[k * dim + j]
            c[i * dim + j] = temp
    
    time2 = time.time() 
            
    output = "Time: {time:.3f} seconds\n"
    print(output.format(time = time2-time1))

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix: \n")

    printMatrix(dim, c)

def OnMultLine(dim):
    a = [1.0] * dim ** 2
    b = [i + 1.0 for i in range(dim) for j in range(dim)]
    c = [0.0] * dim ** 2
    
    time1 = time.time()

    for i in range(dim):
        for k in range(dim):
            for j in range(dim):
                c[i * dim + j] += a[i * dim + k] * b[k * dim + j]
    
    time2 = time.time() 
            
    output = "Time: {time:.3f} seconds\n"
    print(output.format(time = time2-time1))

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix: \n")

    printMatrix(dim, c)

print("1. Multiplication\n")
print("2. Line Multiplication\n")

op = int(input("Selection: "))
dim = int(input("Matrix dimensions (dim x dim): "))

if op == 1:
    OnMult(dim)
elif op == 2:
    OnMultLine(dim)
else:
    print("Invalid option\n")

