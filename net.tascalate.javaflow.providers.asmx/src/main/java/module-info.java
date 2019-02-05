								module net.tascalate.javaflow.providers.asmx {
								    requires org.objectweb.asm;
								    requires org.objectweb.asm.tree; 
								    requires org.objectweb.asm.tree.analysis;

								    requires transitive net.tascalate.javaflow.spi;

								    exports org.apache.commons.javaflow.providers.asmx;
								}
