import { Search, Plus, Filter } from 'lucide-react';
import { Input } from './ui/input';
import { Button } from './ui/button';
import { ScrollArea } from './ui/scroll-area';

interface Product {
  id: string;
  name: string;
  code: string;
  brand: string;
  variants: number;
  image?: string;
}

interface ProductSidebarProps {
  products: Product[];
  selectedProductId: string | null;
  onSelectProduct: (id: string) => void;
  onNewProduct: () => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export function ProductSidebar({
  products,
  selectedProductId,
  onSelectProduct,
  onNewProduct,
  searchQuery,
  onSearchChange
}: ProductSidebarProps) {
  const filteredProducts = products.filter(p => 
    p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.brand.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="w-[250px] h-screen border-r border-gray-200 flex flex-col" style={{ backgroundColor: '#ECF0F1' }}>
      {/* Header */}
      <div className="p-4 border-b border-gray-200" style={{ backgroundColor: '#2C3E50' }}>
        <h2 className="text-white font-semibold mb-3">Productos</h2>
        <Button 
          onClick={onNewProduct}
          className="w-full"
          style={{ backgroundColor: '#3498DB' }}
        >
          <Plus className="w-4 h-4 mr-2" />
          Nuevo Producto
        </Button>
      </div>

      {/* Search */}
      <div className="p-3 border-b border-gray-200">
        <div className="relative">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-gray-400" />
          <Input
            placeholder="Buscar productos..."
            className="pl-8 text-sm"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
        <Button variant="ghost" size="sm" className="w-full mt-2 text-xs">
          <Filter className="w-3 h-3 mr-1" />
          Filtros avanzados
        </Button>
      </div>

      {/* Product List */}
      <ScrollArea className="flex-1">
        <div className="p-2">
          {filteredProducts.map((product) => (
            <div
              key={product.id}
              onClick={() => onSelectProduct(product.id)}
              className={`p-3 mb-2 rounded cursor-pointer transition-all hover:shadow-md ${
                selectedProductId === product.id 
                  ? 'bg-white border-2 shadow-md' 
                  : 'bg-white/70 border border-gray-200'
              }`}
              style={selectedProductId === product.id ? { borderColor: '#3498DB' } : {}}
            >
              <div className="flex items-start gap-2">
                <div className="w-10 h-10 rounded bg-gray-200 flex-shrink-0 flex items-center justify-center text-gray-400 text-xs">
                  {product.image ? (
                    <img src={product.image} alt={product.name} className="w-full h-full object-cover rounded" />
                  ) : (
                    'IMG'
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium text-sm truncate">{product.name}</h3>
                  <p className="text-xs text-gray-500">{product.code}</p>
                  <div className="flex items-center justify-between mt-1">
                    <span className="text-xs text-gray-400">{product.brand}</span>
                    <span className="text-xs px-1.5 py-0.5 rounded" style={{ backgroundColor: '#3498DB20', color: '#3498DB' }}>
                      {product.variants} vars
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </ScrollArea>
    </div>
  );
}
