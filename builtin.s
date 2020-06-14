	.file	"builtin-function.c"
	.option nopic
	.text
	.section	.rodata.str1.4,"aMS",@progbits,1
	.align	2
.LC0:
	.string	"%s"
	.text
	.align	1
	.globl	print
	.type	print, @function
print:
	mv	a1,a0
	lui	a0,%hi(.LC0)
	addi	a0,a0,%lo(.LC0)
	tail	printf
	.size	print, .-print
	.align	1
	.globl	println
	.type	println, @function
println:
	tail	puts
	.size	println, .-println
	.section	.rodata.str1.4
	.align	2
.LC1:
	.string	"%d"
	.text
	.align	1
	.globl	printInt
	.type	printInt, @function
printInt:
	mv	a1,a0
	lui	a0,%hi(.LC1)
	addi	a0,a0,%lo(.LC1)
	tail	printf
	.size	printInt, .-printInt
	.section	.rodata.str1.4
	.align	2
.LC2:
	.string	"%d\n"
	.text
	.align	1
	.globl	printlnInt
	.type	printlnInt, @function
printlnInt:
	mv	a1,a0
	lui	a0,%hi(.LC2)
	addi	a0,a0,%lo(.LC2)
	tail	printf
	.size	printlnInt, .-printlnInt
	.align	1
	.globl	getString
	.type	getString, @function
getString:
	addi	sp,sp,-16
	li	a0,257
	sw	ra,12(sp)
	sw	s0,8(sp)
	call	malloc
	mv	s0,a0
	mv	a1,a0
	lui	a0,%hi(.LC0)
	addi	a0,a0,%lo(.LC0)
	call	__isoc99_scanf
	lw	ra,12(sp)
	mv	a0,s0
	lw	s0,8(sp)
	addi	sp,sp,16
	jr	ra
	.size	getString, .-getString
	.align	1
	.globl	getInt
	.type	getInt, @function
getInt:
	addi	sp,sp,-32
	lui	a0,%hi(.LC1)
	addi	a1,sp,12
	addi	a0,a0,%lo(.LC1)
	sw	ra,28(sp)
	call	__isoc99_scanf
	lw	ra,28(sp)
	lw	a0,12(sp)
	addi	sp,sp,32
	jr	ra
	.size	getInt, .-getInt
	.align	1
	.globl	toString
	.type	toString, @function
toString:
	addi	sp,sp,-48
	sw	ra,44(sp)
	sw	s0,40(sp)
	sw	s1,36(sp)
	sw	s2,32(sp)
	sw	s3,28(sp)
	beq	a0,zero,.L27
	li	s1,0
	li	s2,0
	ble	a0,zero,.L28
.L13:
	li	s0,0
	li	a5,10
.L14:
	rem	a3,a0,a5
	addi	a4,sp,16
	add	a4,a4,s0
	addi	s0,s0,1
	andi	s0,s0,0xff
	div	a0,a0,a5
	sb	a3,-12(a4)
	bne	a0,zero,.L14
	add	s3,s1,s0
	addi	a0,s3,1
	call	malloc
	bne	s2,zero,.L29
.L16:
	beq	s0,zero,.L19
	addi	a5,sp,16
	add	a3,a5,s0
	lbu	a4,-13(a3)
	add	a5,a0,s1
	li	a2,1
	addi	a4,a4,48
	sb	a4,0(a5)
	beq	s0,a2,.L19
	lbu	a4,-14(a3)
	li	a2,2
	addi	a4,a4,48
	sb	a4,1(a5)
	beq	s0,a2,.L19
	lbu	a4,-15(a3)
	li	a2,3
	addi	a4,a4,48
	sb	a4,2(a5)
	beq	s0,a2,.L19
	lbu	a4,-16(a3)
	li	a2,4
	addi	a4,a4,48
	sb	a4,3(a5)
	beq	s0,a2,.L19
	lbu	a4,-17(a3)
	li	a2,5
	addi	a4,a4,48
	sb	a4,4(a5)
	beq	s0,a2,.L19
	lbu	a4,-18(a3)
	li	a2,6
	addi	a4,a4,48
	sb	a4,5(a5)
	beq	s0,a2,.L19
	lbu	a4,-19(a3)
	li	a2,7
	addi	a4,a4,48
	sb	a4,6(a5)
	beq	s0,a2,.L19
	lbu	a4,-20(a3)
	li	a2,8
	addi	a4,a4,48
	sb	a4,7(a5)
	beq	s0,a2,.L19
	lbu	a4,-21(a3)
	li	a2,9
	addi	a4,a4,48
	sb	a4,8(a5)
	beq	s0,a2,.L19
	lbu	a4,-22(a3)
	addi	a4,a4,48
	sb	a4,9(a5)
.L19:
	add	a5,a0,s3
	sb	zero,0(a5)
	lw	ra,44(sp)
	lw	s0,40(sp)
	lw	s1,36(sp)
	lw	s2,32(sp)
	lw	s3,28(sp)
	addi	sp,sp,48
	jr	ra
.L29:
	li	a5,45
	sb	a5,0(a0)
	j	.L16
.L28:
	neg	a0,a0
	li	s1,1
	li	s2,1
	j	.L13
.L27:
	li	a0,2
	call	malloc
	li	a5,48
	sh	a5,0(a0)
	lw	ra,44(sp)
	lw	s0,40(sp)
	lw	s1,36(sp)
	lw	s2,32(sp)
	lw	s3,28(sp)
	addi	sp,sp,48
	jr	ra
	.size	toString, .-toString
	.align	1
	.globl	__string_concatenate
	.type	__string_concatenate, @function
__string_concatenate:
	addi	sp,sp,-32
	sw	s2,16(sp)
	sw	s3,12(sp)
	sw	ra,28(sp)
	sw	s0,24(sp)
	sw	s1,20(sp)
	sw	s4,8(sp)
	sw	s5,4(sp)
	lbu	a5,0(a0)
	mv	s2,a0
	mv	s3,a1
	beq	a5,zero,.L31
	li	s0,0
.L32:
	mv	a0,s0
	addi	s0,s0,1
	add	a5,s2,s0
	lbu	a5,0(a5)
	bne	a5,zero,.L32
	lbu	a5,0(s3)
	beq	a5,zero,.L36
.L34:
	li	s1,0
.L37:
	mv	s4,s1
	addi	s1,s1,1
	add	a5,s3,s1
	lbu	a5,0(a5)
	bne	a5,zero,.L37
	add	a0,s1,s0
	addi	a0,a0,1
	call	malloc
	mv	s5,a0
	beq	s0,zero,.L46
	li	a2,1
	ble	s0,zero,.L40
	mv	a2,s0
.L40:
	mv	a1,s2
	mv	a0,s5
	call	memcpy
.L46:
	add	a0,s5,s0
	li	a2,1
	ble	s1,zero,.L42
	mv	a2,s1
.L42:
	mv	a1,s3
	call	memcpy
	addi	s0,s0,1
	li	a5,0
	ble	s1,zero,.L44
	mv	a5,s4
.L44:
	add	s0,a5,s0
	add	s0,s5,s0
.L35:
	sb	zero,0(s0)
	lw	ra,28(sp)
	lw	s0,24(sp)
	lw	s1,20(sp)
	lw	s2,16(sp)
	lw	s3,12(sp)
	lw	s4,8(sp)
	mv	a0,s5
	lw	s5,4(sp)
	addi	sp,sp,32
	jr	ra
.L36:
	addi	a0,a0,2
	call	malloc
	mv	s5,a0
	li	a2,1
	ble	s0,zero,.L48
	mv	a2,s0
.L48:
	mv	a1,s2
	mv	a0,s5
	call	memcpy
	add	s0,s5,s0
	j	.L35
.L31:
	lbu	a5,0(a1)
	li	s0,0
	bne	a5,zero,.L34
	li	a0,1
	call	malloc
	mv	s5,a0
	mv	s0,a0
	j	.L35
	.size	__string_concatenate, .-__string_concatenate
	.align	1
	.globl	__string_equal
	.type	__string_equal, @function
__string_equal:
	lbu	a4,0(a0)
	lbu	a5,0(a1)
	beq	a4,zero,.L57
	addi	a0,a0,1
	j	.L58
.L60:
	bne	a5,a4,.L61
	lbu	a4,0(a0)
	lbu	a5,1(a1)
	addi	a0,a0,1
	addi	a1,a1,1
	beq	a4,zero,.L57
.L58:
	lbu	a5,0(a1)
	bne	a5,zero,.L60
.L57:
	sub	a0,a4,a5
	seqz	a0,a0
	ret
.L61:
	li	a0,0
	ret
	.size	__string_equal, .-__string_equal
	.align	1
	.globl	__string_notEqual
	.type	__string_notEqual, @function
__string_notEqual:
	lbu	a4,0(a0)
	lbu	a5,0(a1)
	beq	a4,zero,.L69
	addi	a0,a0,1
	j	.L70
.L72:
	bne	a5,a4,.L73
	lbu	a4,0(a0)
	lbu	a5,1(a1)
	addi	a0,a0,1
	addi	a1,a1,1
	beq	a4,zero,.L69
.L70:
	lbu	a5,0(a1)
	bne	a5,zero,.L72
.L69:
	sub	a0,a4,a5
	snez	a0,a0
	ret
.L73:
	li	a0,1
	ret
	.size	__string_notEqual, .-__string_notEqual
	.align	1
	.globl	__string_lessThan
	.type	__string_lessThan, @function
__string_lessThan:
	lbu	a4,0(a0)
	mv	a6,a0
	li	a5,0
	bne	a4,zero,.L81
	j	.L82
.L84:
	bgtu	a0,a4,.L86
	bltu	a0,a4,.L87
	lbu	a4,0(a2)
	beq	a4,zero,.L91
.L81:
	add	a3,a1,a5
	lbu	a0,0(a3)
	addi	a5,a5,1
	add	a2,a6,a5
	bne	a0,zero,.L84
	ret
.L91:
	add	a1,a1,a5
.L82:
	lbu	a0,0(a1)
	snez	a0,a0
	ret
.L86:
	li	a0,1
	ret
.L87:
	li	a0,0
	ret
	.size	__string_lessThan, .-__string_lessThan
	.align	1
	.globl	__string_greaterThan
	.type	__string_greaterThan, @function
__string_greaterThan:
	mv	a4,a0
	lbu	a0,0(a0)
	beq	a0,zero,.L93
	addi	a4,a4,1
	j	.L94
.L95:
	bgtu	a5,a0,.L96
	bltu	a5,a0,.L97
	lbu	a0,0(a4)
	addi	a4,a4,1
	beq	a0,zero,.L93
.L94:
	lbu	a5,0(a1)
	addi	a1,a1,1
	bne	a5,zero,.L95
.L97:
	li	a0,1
.L93:
	ret
.L96:
	li	a0,0
	ret
	.size	__string_greaterThan, .-__string_greaterThan
	.align	1
	.globl	__string_lessEqual
	.type	__string_lessEqual, @function
__string_lessEqual:
	lbu	a5,0(a0)
	beq	a5,zero,.L111
	addi	a4,a0,1
	j	.L106
.L107:
	bgtu	a0,a5,.L111
	bltu	a0,a5,.L110
	lbu	a5,0(a4)
	addi	a4,a4,1
	beq	a5,zero,.L111
.L106:
	lbu	a0,0(a1)
	addi	a1,a1,1
	bne	a0,zero,.L107
	ret
.L111:
	li	a0,1
	ret
.L110:
	li	a0,0
	ret
	.size	__string_lessEqual, .-__string_lessEqual
	.align	1
	.globl	__string_greaterEqual
	.type	__string_greaterEqual, @function
__string_greaterEqual:
	lbu	a4,0(a0)
	li	a5,0
	bne	a4,zero,.L113
	j	.L114
.L116:
	bgtu	a3,a4,.L118
	bltu	a3,a4,.L119
	lbu	a4,0(a2)
	beq	a4,zero,.L123
.L113:
	add	a3,a1,a5
	lbu	a3,0(a3)
	addi	a5,a5,1
	add	a2,a0,a5
	bne	a3,zero,.L116
.L119:
	li	a0,1
	ret
.L123:
	add	a1,a1,a5
.L114:
	lbu	a0,0(a1)
	seqz	a0,a0
	ret
.L118:
	li	a0,0
	ret
	.size	__string_greaterEqual, .-__string_greaterEqual
	.align	1
	.globl	__string_length
	.type	__string_length, @function
__string_length:
	lbu	a5,0(a0)
	mv	a4,a0
	li	a0,0
	beq	a5,zero,.L127
.L126:
	addi	a0,a0,1
	add	a5,a4,a0
	lbu	a5,0(a5)
	bne	a5,zero,.L126
	ret
.L127:
	ret
	.size	__string_length, .-__string_length
	.align	1
	.globl	__string_substring
	.type	__string_substring, @function
__string_substring:
	addi	sp,sp,-32
	sw	s0,24(sp)
	sub	s0,a2,a1
	sw	s3,12(sp)
	mv	s3,a0
	addi	a0,s0,1
	sw	s1,20(sp)
	sw	s2,16(sp)
	sw	ra,28(sp)
	mv	s2,a1
	call	malloc
	mv	s1,a0
	ble	s0,zero,.L130
	mv	a2,s0
	add	a1,s3,s2
	call	memcpy
.L130:
	add	s0,s1,s0
	sb	zero,0(s0)
	lw	ra,28(sp)
	lw	s0,24(sp)
	lw	s2,16(sp)
	lw	s3,12(sp)
	mv	a0,s1
	lw	s1,20(sp)
	addi	sp,sp,32
	jr	ra
	.size	__string_substring, .-__string_substring
	.align	1
	.globl	__string_parseInt
	.type	__string_parseInt, @function
__string_parseInt:
	lbu	a2,0(a0)
	li	a4,9
	addi	a5,a2,-48
	andi	a5,a5,0xff
	bgtu	a5,a4,.L135
	addi	a3,a0,1
	li	a1,9
	li	a0,0
.L134:
	slli	a5,a0,2
	add	a0,a5,a0
	slli	a0,a0,1
	add	a0,a0,a2
	lbu	a2,0(a3)
	addi	a0,a0,-48
	addi	a3,a3,1
	addi	a4,a2,-48
	andi	a4,a4,0xff
	bleu	a4,a1,.L134
	ret
.L135:
	li	a0,0
	ret
	.size	__string_parseInt, .-__string_parseInt
	.align	1
	.globl	__string_ord
	.type	__string_ord, @function
__string_ord:
	add	a0,a0,a1
	lbu	a0,0(a0)
	ret
	.size	__string_ord, .-__string_ord
	.align	1
	.globl	__array_size
	.type	__array_size, @function
__array_size:
	lw	a0,-4(a0)
	ret
	.size	__array_size, .-__array_size
	.ident	"GCC: (GNU) 10.1.0"
	.section	.note.GNU-stack,"",@progbits
