package org.pavanecce.uml.uml2java.collections.tests;

import org.pavanecce.uml.uml2java.collections.ManyToManyCollection;
import org.pavanecce.uml.uml2java.collections.ManyToManySet;

public class ManyFrom {

	protected ManyToManySet<ManyFrom, ManyTo> many = new ManyToManySet<ManyFrom, ManyTo>(this) {

		private static final long serialVersionUID = 4411297325486752356L;

		@Override
		protected boolean isLoaded() {
			return false;
		}

		@Override
		protected ManyToManyCollection<ManyTo, ManyFrom> getOtherEnd(ManyTo child) {
			return child.many;
		}

		@Override
		protected boolean isInstanceOfChild(Object o) {
			return o instanceof ManyTo;
		}
	};

}
