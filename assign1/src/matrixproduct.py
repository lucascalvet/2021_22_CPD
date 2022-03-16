import time

def OnMult(dim):
    a = []
    b = []
    c = []
    
    for i in range(dim):
        for j in range(dim):
            a[i * dim + j] = 1.0
    
    for i in range(dim):
        for j in range(dim):
            b[i * dim + j] = i + 1.0
    
    time1 = time.time()

    for i in range(dim):
        for j in range(dim):
            temp = 0
            for k in range(dim):
                temp += a[i * dim + k] * b[k * dim + j]
            c[i * dim + j] = temp
    
    time2 = time.time() 
            
    output = "Time: {time:.3f} seconds\n"
    print(output.format(time2-time1))

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix: \n")

    for i in range(min(10, dim)):
        print(c[i] + " ")
    print("\n")

def OnMultLine(dim):
    a = []
    b = []
    c = []
    
    for i in range(dim):
        for j in range(dim):
            a[i * dim + j] = 1.0
    
    for i in range(dim):
        for j in range(dim):
            b[i * dim + j] = i + 1.0
    
    time1 = time.time()

    for i in range(dim):
        for k in range(dim):
            temp = 0
            for j in range(dim):
                temp += a[i * dim + k] * b[k * dim + j]
            c[i * dim + j] = temp
    
    time2 = time.time() 
            
    output = "Time: {time:.3f} seconds\n"
    print(output.format(time2-time1))

    # display 10 elements of the result matrix to verify correctness
    print("Result matrix: \n")

    for i in range(min(10, dim)):
        print(c[i] + " ")
    print("\n")


print("1. Multiplication\n")
print("2. Line Multiplication\n")
#print("3. Block Multiplication\n")
op = input("Selection: ")
dim = input("Matrix dimensions (dim x dim): ")

if op == 1:
    OnMult(dim)
elif op == 2:
    OnMultLine(dim)
#elif op == 3:
    #OnMultiBlock(dim)

