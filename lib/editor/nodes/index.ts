import { registry } from "../registry";

import { paragraphDef } from "./paragraph";
import { headingDef } from "./heading";
import { blockquoteDef } from "./blockquote";
import { quoteBlockDef } from "./quote-block";
import { codeBlockDef } from "./code-block";
import { bulletedListDef, numberedListDef, listItemDef } from "./list";
import { linkDef } from "./link";
import { imageDef } from "./image";
import { placeholderDef } from "./placeholder";
import { inlineEditorDef } from "./inline-editor";
import { allMarkDefs } from "./marks";
import { tableCellDef, tableDef, tableRowDef } from "./table";
import { rollableEditorDef } from "./rollable";
import { diceDef } from "./dice";

// Register all block/inline defs
registry.register(paragraphDef);
registry.register(headingDef);
registry.register(blockquoteDef);
registry.register(quoteBlockDef);
registry.register(codeBlockDef);
registry.register(bulletedListDef);
registry.register(numberedListDef);
registry.register(listItemDef);
registry.register(linkDef);
registry.register(imageDef);
registry.register(placeholderDef);
registry.register(diceDef);
registry.register(inlineEditorDef);
registry.register(tableDef);
registry.register(tableCellDef);
registry.register(tableRowDef);
registry.register(rollableEditorDef);
// Register marks
for (const mark of allMarkDefs) {
  registry.register(mark);
}

export { registry };
