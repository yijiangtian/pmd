package net.sourceforge.pmd.lang.java.ast

import net.sourceforge.pmd.lang.ast.test.shouldBe
import net.sourceforge.pmd.lang.java.ast.ASTPrimitiveType.PrimitiveType.INT
import net.sourceforge.pmd.lang.java.ast.ParserTestCtx.Companion.ExpressionParsingCtx

/**
 * Nodes that previously corresponded to ASTAllocationExpression.
 *
 * @author Clément Fournier
 * @since 7.0.0
 */
class ASTMethodReferenceTest : ParserTestSpec({

    parserTest("Method reference") {

        inContext(ExpressionParsingCtx) {

            "this::foo" should parseAs {

                methodRef("foo") {
                    it::getTypeArguments shouldBe null

                    it::getQualifier shouldBe thisExpr()
                }
            }

            "foobar.b::foo" should parseAs {

                methodRef("foo") {
                    it::getTypeArguments shouldBe null

                    it::getQualifier shouldBe ambiguousName("foobar.b")
                }
            }

            "foobar.b::<B>foo" should parseAs {

                methodRef("foo") {
                    it::getQualifier shouldBe ambiguousName("foobar.b")

                    it::getTypeArguments shouldBe child {
                        classType("B")
                    }
                }
            }


            "foobar.b<B>::foo" should parseAs {

                methodRef("foo") {
                    it::getTypeArguments shouldBe null

                    it::getQualifier shouldBe typeExpr {
                        classType("b") {

                            it::getAmbiguousLhs shouldBe child {
                                it::getName shouldBe "foobar"
                            }

                            it::getTypeArguments shouldBe child {
                                child<ASTClassOrInterfaceType> {
                                    it::getTypeImage shouldBe "B"
                                }
                            }
                        }
                    }
                }
            }

            "java.util.Map<String, String>.Entry<String, String>::foo" should parseAs {

                methodRef("foo") {
                    it::getQualifier shouldBe typeExpr {
                        classType("Entry") // ignore the rest
                    }
                }
            }

            "super::foo" should parseAs {
                methodRef("foo") {
                    it::getQualifier shouldBe child<ASTSuperExpression> {

                    }
                }
            }

            "T.B.super::foo" should parseAs {
                methodRef("foo") {
                    it::getQualifier shouldBe child<ASTSuperExpression> {
                        it::getQualifier shouldBe classType("B") {
                            ambiguousName("T")
                        }
                    }
                }
            }

        }

    }

    parserTest("Neg tests") {

        inContext(ExpressionParsingCtx) {

            "foo::bar::bar" shouldNot parse()
            "foo::bar.foo()" shouldNot parse()
            "foo::bar.foo" shouldNot parse()

        }

    }

    parserTest("Constructor reference") {

        inContext(ExpressionParsingCtx) {

            "foobar.b::new" should parseAs {
                constructorRef {
                    it::getTypeArguments shouldBe null

                    typeExpr {
                        classType("b") {
                            it::getAmbiguousLhs shouldBe ambiguousName("foobar")
                        }
                    }
                }
            }

            "foobar.b<B>::new" should parseAs {
                constructorRef {
                    it::getTypeArguments shouldBe null


                    typeExpr {
                        classType("b") {
                            it::getAmbiguousLhs shouldBe ambiguousName("foobar")
                            it::getTypeArguments shouldBe typeArgList {
                                classType("B")
                            }
                        }
                    }
                }
            }

            "int[]::new" should parseAs {
                constructorRef {
                    it::getTypeArguments shouldBe null

                    typeExpr {
                        arrayType {
                            primitiveType(INT)
                            it::getDimensions shouldBe child {
                                arrayDim()
                            }
                        }
                    }
                }
            }

            "Class<?>[]::new" should parseAs {
                constructorRef {
                    it::getTypeArguments shouldBe null

                    typeExpr {
                        arrayType {
                            classType("Class") {
                                typeArgList {
                                    child<ASTWildcardType> { }
                                }
                            }
                            it::getDimensions shouldBe child {
                                arrayDim()
                            }
                        }
                    }
                }
            }

            "ArrayList::<String>new" should parseAs {
                constructorRef {
                    val lhs = typeExpr {
                        classType("ArrayList")
                    }
                    it::getTypeArguments shouldBe typeArgList {
                        classType("String")
                    }

                    lhs
                }
            }
        }
    }

    parserTest("Type annotations") {

        inContext(ExpressionParsingCtx) {

            "@Vernal Date::getDay" should parseAs {
                methodRef(methodName = "getDay") {
                    it::getQualifier shouldBe typeExpr {
                        classType("Date") {
                            annotation("Vernal")
                        }
                    }

                    it::getTypeArguments shouldBe null
                }
            }

            // annotated method ref in cast ctx (lookahead trickery)
            "(Foo) @Vernal Date::getDay" should parseAs {
                castExpr {
                    classType("Foo")

                    methodRef(methodName = "getDay") {
                        it::getQualifier shouldBe typeExpr {
                            classType("Date") {
                                annotation("Vernal")
                            }
                        }

                        it::getTypeArguments shouldBe null
                    }
                }
            }
        }
    }
})