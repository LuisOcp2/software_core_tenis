import { useState, useEffect } from 'react';
import { ChevronRight, Save, Check } from 'lucide-react';
import { ProductSidebar } from './components/ProductSidebar';
import { GeneralInfoTab } from './components/GeneralInfoTab';
import { VariantsTab, Variant } from './components/VariantsTab';
import { InventoryTab } from './components/InventoryTab';
import { Tabs, TabsList, TabsTrigger, TabsContent } from './components/ui/tabs';
import { Button } from './components/ui/button';
import { Breadcrumb, BreadcrumbList, BreadcrumbItem, BreadcrumbLink, BreadcrumbSeparator, BreadcrumbPage } from './components/ui/breadcrumb';

interface Product {
  id: string;
  name: string;
  code: string;
  brand: string;
  variants: number;
  image?: string;
}

interface GeneralInfo {
  name: string;
  code: string;
  description: string;
  category: string;
  brand: string;
  supplier: string;
  gender: string;
  basePurchasePrice: string;
  baseSalePrice: string;
  minStock: string;
  active: boolean;
}

export default function App() {
  const [products, setProducts] = useState<Product[]>([
    {
      id: '1',
      name: 'Adidas EQ21',
      code: 'ADI-EQ21-001',
      brand: 'Adidas',
      variants: 45
    },
    {
      id: '2',
      name: 'Nike Air Max 270',
      code: 'NIKE-AM270-002',
      brand: 'Nike',
      variants: 33
    },
    {
      id: '3',
      name: 'Puma RS-X',
      code: 'PUMA-RSX-003',
      brand: 'Puma',
      variants: 28
    }
  ]);

  const [selectedProductId, setSelectedProductId] = useState<string | null>('1');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('general');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const [generalInfo, setGeneralInfo] = useState<GeneralInfo>({
    name: 'Adidas EQ21',
    code: 'ADI-EQ21-001',
    description: 'Zapatilla deportiva de alto rendimiento con tecnología Boost. Ideal para running y entrenamiento intensivo.',
    category: 'deportivo',
    brand: 'adidas',
    supplier: 'proveedor-1',
    gender: 'unisex',
    basePurchasePrice: '45.00',
    baseSalePrice: '89.99',
    minStock: '10',
    active: true
  });

  const [variants, setVariants] = useState<Variant[]>([
    {
      id: 'var-1',
      size: '39',
      color: 'Negro',
      colorHex: '#000000',
      sku: 'SKU-39-NEG-A1B2',
      ean: '7501234567890',
      purchasePrice: '45.00',
      salePrice: '89.99',
      margin: 100,
      minStock: '5',
      supplier: 'proveedor-1',
      active: true,
      currentStock: 24
    },
    {
      id: 'var-2',
      size: '40',
      color: 'Negro',
      colorHex: '#000000',
      sku: 'SKU-40-NEG-C3D4',
      ean: '7501234567891',
      purchasePrice: '45.00',
      salePrice: '89.99',
      margin: 100,
      minStock: '5',
      supplier: 'proveedor-1',
      active: true,
      currentStock: 18
    },
    {
      id: 'var-3',
      size: '39',
      color: 'Blanco',
      colorHex: '#FFFFFF',
      sku: 'SKU-39-BLA-E5F6',
      ean: '7501234567892',
      purchasePrice: '45.00',
      salePrice: '89.99',
      margin: 100,
      minStock: '5',
      supplier: 'proveedor-1',
      active: true,
      currentStock: 30
    }
  ]);

  const handleGeneralInfoChange = (field: string, value: any) => {
    setGeneralInfo(prev => ({ ...prev, [field]: value }));
    simulateSave();
  };

  const handleVariantsChange = (newVariants: Variant[]) => {
    setVariants(newVariants);
    simulateSave();
  };

  const simulateSave = () => {
    setSaving(true);
    setSaved(false);
    setTimeout(() => {
      setSaving(false);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    }, 1000);
  };

  const handleNewProduct = () => {
    const newProduct: Product = {
      id: `prod-${Date.now()}`,
      name: 'Nuevo Producto',
      code: `PROD-${Date.now()}`,
      brand: 'Sin marca',
      variants: 0
    };
    setProducts([...products, newProduct]);
    setSelectedProductId(newProduct.id);
  };

  const selectedProduct = products.find(p => p.id === selectedProductId);

  return (
    <div className="flex h-screen overflow-hidden" style={{ backgroundColor: '#F8F9FA' }}>
      {/* Sidebar */}
      <ProductSidebar
        products={products}
        selectedProductId={selectedProductId}
        onSelectProduct={setSelectedProductId}
        onNewProduct={handleNewProduct}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
      />

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="border-b px-6 py-4" style={{ backgroundColor: '#2C3E50' }}>
          <div className="flex items-center justify-between">
            <div>
              <Breadcrumb>
                <BreadcrumbList>
                  <BreadcrumbItem>
                    <BreadcrumbLink className="text-gray-300 hover:text-white">
                      Productos
                    </BreadcrumbLink>
                  </BreadcrumbItem>
                  <BreadcrumbSeparator className="text-gray-400">
                    <ChevronRight className="w-4 h-4" />
                  </BreadcrumbSeparator>
                  <BreadcrumbItem>
                    <BreadcrumbLink className="text-gray-300 hover:text-white">
                      Calzado Deportivo
                    </BreadcrumbLink>
                  </BreadcrumbItem>
                  <BreadcrumbSeparator className="text-gray-400">
                    <ChevronRight className="w-4 h-4" />
                  </BreadcrumbSeparator>
                  <BreadcrumbItem>
                    <BreadcrumbPage className="text-white font-medium">
                      {selectedProduct?.name || 'Adidas EQ21'}
                    </BreadcrumbPage>
                  </BreadcrumbItem>
                </BreadcrumbList>
              </Breadcrumb>
              <h1 className="text-2xl font-semibold text-white mt-2">
                {selectedProduct?.name || 'Adidas EQ21'}
              </h1>
            </div>
            <div className="flex items-center gap-3">
              {saving && (
                <div className="flex items-center gap-2 text-white text-sm">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Guardando...
                </div>
              )}
              {saved && (
                <div className="flex items-center gap-2 text-green-300 text-sm">
                  <Check className="w-4 h-4" />
                  Guardado ✓
                </div>
              )}
              <Button style={{ backgroundColor: '#3498DB' }} className="px-6">
                <Save className="w-4 h-4 mr-2" />
                Guardar Cambios
              </Button>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 overflow-y-auto p-6">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="mb-6">
              <TabsTrigger value="general">Información General</TabsTrigger>
              <TabsTrigger value="variants">Gestión de Variantes</TabsTrigger>
              <TabsTrigger value="inventory">Inventario por Bodega</TabsTrigger>
            </TabsList>

            <TabsContent value="general">
              <GeneralInfoTab data={generalInfo} onChange={handleGeneralInfoChange} />
            </TabsContent>

            <TabsContent value="variants">
              <VariantsTab variants={variants} onVariantsChange={handleVariantsChange} />
            </TabsContent>

            <TabsContent value="inventory">
              <InventoryTab variants={variants} />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
