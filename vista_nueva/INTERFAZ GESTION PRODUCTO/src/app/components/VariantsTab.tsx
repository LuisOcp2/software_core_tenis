import { useState } from 'react';
import { Plus, Upload, FileSpreadsheet, Edit2, Copy, Trash2, AlertTriangle, Sparkles } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Switch } from './ui/switch';
import { Badge } from './ui/badge';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from './ui/tooltip';
import { MassGeneratorModal, MassGeneratorConfig } from './MassGeneratorModal';

export interface Variant {
  id: string;
  image?: string;
  size: string;
  color: string;
  colorHex: string;
  sku: string;
  ean: string;
  purchasePrice: string;
  salePrice: string;
  margin: number;
  minStock: string;
  supplier: string;
  active: boolean;
  isNew?: boolean;
  currentStock?: number;
}

interface VariantsTabProps {
  variants: Variant[];
  onVariantsChange: (variants: Variant[]) => void;
}

const COLORS_MAP: Record<string, string> = {
  'negro': '#000000',
  'blanco': '#FFFFFF',
  'rojo': '#DC2626',
  'azul': '#2563EB',
  'verde': '#16A34A',
  'gris': '#6B7280',
  'amarillo': '#EAB308',
  'naranja': '#EA580C',
  'rosa': '#EC4899',
  'marron': '#92400E',
};

export function VariantsTab({ variants, onVariantsChange }: VariantsTabProps) {
  const [showMassGenerator, setShowMassGenerator] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [editingCell, setEditingCell] = useState<{ id: string; field: string } | null>(null);
  const itemsPerPage = 20;

  const calculateMargin = (purchasePrice: string, salePrice: string): number => {
    const purchase = parseFloat(purchasePrice) || 0;
    const sale = parseFloat(salePrice) || 0;
    if (purchase === 0) return 0;
    return ((sale - purchase) / purchase) * 100;
  };

  const generateSKU = (size: string, color: string): string => {
    const colorCode = color.substring(0, 3).toUpperCase();
    return `SKU-${size}-${colorCode}-${Math.random().toString(36).substring(2, 6).toUpperCase()}`;
  };

  const handleMassGenerate = (config: MassGeneratorConfig) => {
    const newVariants: Variant[] = [];
    
    config.sizes.forEach(size => {
      config.colors.forEach(colorId => {
        const variant: Variant = {
          id: `var-${Date.now()}-${Math.random()}`,
          size,
          color: colorId.charAt(0).toUpperCase() + colorId.slice(1),
          colorHex: COLORS_MAP[colorId] || '#CCCCCC',
          sku: generateSKU(size, colorId),
          ean: '',
          purchasePrice: config.basePurchasePrice,
          salePrice: config.baseSalePrice,
          margin: calculateMargin(config.basePurchasePrice, config.baseSalePrice),
          minStock: config.minStock,
          supplier: config.supplier,
          active: true,
          isNew: true,
          currentStock: 0
        };
        newVariants.push(variant);
      });
    });

    onVariantsChange([...variants, ...newVariants]);
    setShowMassGenerator(false);
  };

  const handleAddVariant = () => {
    const newVariant: Variant = {
      id: `var-${Date.now()}`,
      size: '39',
      color: 'Negro',
      colorHex: '#000000',
      sku: generateSKU('39', 'negro'),
      ean: '',
      purchasePrice: '',
      salePrice: '',
      margin: 0,
      minStock: '5',
      supplier: '',
      active: true,
      isNew: true,
      currentStock: 0
    };
    onVariantsChange([...variants, newVariant]);
  };

  const handleDuplicateVariant = (variant: Variant) => {
    const duplicated: Variant = {
      ...variant,
      id: `var-${Date.now()}`,
      sku: generateSKU(variant.size, variant.color.toLowerCase()),
      isNew: true
    };
    onVariantsChange([...variants, duplicated]);
  };

  const handleDeleteVariant = (id: string) => {
    onVariantsChange(variants.filter(v => v.id !== id));
  };

  const updateVariant = (id: string, field: string, value: any) => {
    onVariantsChange(variants.map(v => {
      if (v.id === id) {
        const updated = { ...v, [field]: value };
        if (field === 'purchasePrice' || field === 'salePrice') {
          updated.margin = calculateMargin(updated.purchasePrice, updated.salePrice);
        }
        return updated;
      }
      return v;
    }));
  };

  const activeVariants = variants.filter(v => v.active).length;
  const totalValue = variants.reduce((sum, v) => {
    const stock = v.currentStock || 0;
    const price = parseFloat(v.salePrice) || 0;
    return sum + (stock * price);
  }, 0);

  const paginatedVariants = variants.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const totalPages = Math.ceil(variants.length / itemsPerPage);

  return (
    <TooltipProvider>
      <Card className="border-0 shadow-md">
        <CardContent className="p-6">
          {/* Header con Botones */}
          <div className="flex flex-wrap gap-3 mb-6">
            <Button onClick={handleAddVariant} style={{ backgroundColor: '#3498DB' }}>
              <Plus className="w-4 h-4 mr-2" />
              Agregar Variante Individual
            </Button>
            <Button 
              onClick={() => setShowMassGenerator(true)}
              variant="outline"
              className="border-2"
              style={{ borderColor: '#3498DB', color: '#3498DB' }}
            >
              <Sparkles className="w-4 h-4 mr-2" />
              Generador Masivo
            </Button>
            <Button variant="outline">
              <FileSpreadsheet className="w-4 h-4 mr-2" />
              Importar CSV
            </Button>
          </div>

          {/* Tabla de Variantes */}
          <div className="overflow-x-auto border rounded-lg">
            <table className="w-full text-sm">
              <thead style={{ backgroundColor: '#ECF0F1' }}>
                <tr>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Imagen</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Talla</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Color</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">SKU</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">EAN/Código</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">P. Compra</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">P. Venta</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Margen %</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Stock Mín</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Proveedor</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Estado</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-700">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {paginatedVariants.map((variant, index) => (
                  <tr 
                    key={variant.id}
                    className={`border-t hover:bg-gray-50 transition-colors ${
                      index % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
                    }`}
                  >
                    {/* Imagen */}
                    <td className="px-3 py-2">
                      <div className="w-[60px] h-[60px] border-2 border-dashed border-gray-300 rounded flex items-center justify-center text-gray-400 text-xs cursor-pointer hover:border-blue-400">
                        {variant.image ? (
                          <img src={variant.image} alt="Variante" className="w-full h-full object-cover rounded" />
                        ) : (
                          <Upload className="w-5 h-5" />
                        )}
                      </div>
                    </td>

                    {/* Talla */}
                    <td className="px-3 py-2">
                      <Select 
                        value={variant.size} 
                        onValueChange={(val) => updateVariant(variant.id, 'size', val)}
                      >
                        <SelectTrigger className="w-20 h-8">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {Array.from({ length: 11 }, (_, i) => (35 + i).toString()).map(size => (
                            <SelectItem key={size} value={size}>{size}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </td>

                    {/* Color */}
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-2">
                        <div 
                          className="w-6 h-6 rounded-full border-2 border-gray-300"
                          style={{ backgroundColor: variant.colorHex }}
                        />
                        <span className="text-sm">{variant.color}</span>
                      </div>
                    </td>

                    {/* SKU */}
                    <td className="px-3 py-2">
                      <Input
                        value={variant.sku}
                        onChange={(e) => updateVariant(variant.id, 'sku', e.target.value)}
                        className="h-8 text-xs font-mono"
                      />
                    </td>

                    {/* EAN */}
                    <td className="px-3 py-2">
                      <Input
                        value={variant.ean}
                        onChange={(e) => updateVariant(variant.id, 'ean', e.target.value)}
                        className="h-8 text-xs"
                        placeholder="EAN-13"
                      />
                    </td>

                    {/* Precio Compra */}
                    <td className="px-3 py-2">
                      <div className="relative">
                        <span className="absolute left-2 top-1.5 text-gray-500 text-xs">$</span>
                        <Input
                          type="number"
                          value={variant.purchasePrice}
                          onChange={(e) => updateVariant(variant.id, 'purchasePrice', e.target.value)}
                          className="h-8 pl-6 text-xs"
                        />
                      </div>
                    </td>

                    {/* Precio Venta */}
                    <td className="px-3 py-2">
                      <div className="relative">
                        <span className="absolute left-2 top-1.5 text-gray-500 text-xs">$</span>
                        <Input
                          type="number"
                          value={variant.salePrice}
                          onChange={(e) => updateVariant(variant.id, 'salePrice', e.target.value)}
                          className="h-8 pl-6 text-xs"
                        />
                      </div>
                    </td>

                    {/* Margen */}
                    <td className="px-3 py-2">
                      <div 
                        className={`px-2 py-1 rounded text-xs font-semibold text-center ${
                          variant.margin >= 30 ? 'bg-green-100 text-green-700' : 
                          variant.margin >= 15 ? 'bg-yellow-100 text-yellow-700' : 
                          'bg-red-100 text-red-700'
                        }`}
                      >
                        {variant.margin.toFixed(1)}%
                      </div>
                    </td>

                    {/* Stock Mínimo */}
                    <td className="px-3 py-2">
                      <Input
                        type="number"
                        value={variant.minStock}
                        onChange={(e) => updateVariant(variant.id, 'minStock', e.target.value)}
                        className="h-8 w-16 text-xs"
                      />
                    </td>

                    {/* Proveedor */}
                    <td className="px-3 py-2">
                      <Select 
                        value={variant.supplier} 
                        onValueChange={(val) => updateVariant(variant.id, 'supplier', val)}
                      >
                        <SelectTrigger className="w-32 h-8 text-xs">
                          <SelectValue placeholder="Seleccionar" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="proveedor-1">SportWear S.A.</SelectItem>
                          <SelectItem value="proveedor-2">Global Shoes</SelectItem>
                          <SelectItem value="proveedor-3">Premium Ltda</SelectItem>
                        </SelectContent>
                      </Select>
                    </td>

                    {/* Estado */}
                    <td className="px-3 py-2">
                      <Switch
                        checked={variant.active}
                        onCheckedChange={(checked) => updateVariant(variant.id, 'active', checked)}
                      />
                    </td>

                    {/* Acciones */}
                    <td className="px-3 py-2">
                      <div className="flex items-center gap-1">
                        {variant.isNew && (
                          <Badge variant="secondary" className="text-xs px-1.5 py-0.5 bg-blue-100 text-blue-700">
                            Nuevo
                          </Badge>
                        )}
                        {(variant.currentStock || 0) < parseInt(variant.minStock || '0') && (
                          <Tooltip>
                            <TooltipTrigger>
                              <AlertTriangle className="w-4 h-4 text-yellow-500" />
                            </TooltipTrigger>
                            <TooltipContent>Stock bajo</TooltipContent>
                          </Tooltip>
                        )}
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button variant="ghost" size="sm" className="h-7 w-7 p-0">
                              <Edit2 className="w-3.5 h-3.5" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Editar</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="sm" 
                              className="h-7 w-7 p-0"
                              onClick={() => handleDuplicateVariant(variant)}
                            >
                              <Copy className="w-3.5 h-3.5" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Duplicar</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="sm" 
                              className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                              onClick={() => handleDeleteVariant(variant.id)}
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Eliminar</TooltipContent>
                        </Tooltip>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Footer con Totales */}
          <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
            <div className="flex gap-6 text-sm">
              <div>
                <span className="text-gray-500">Total variantes: </span>
                <span className="font-semibold">{variants.length}</span>
              </div>
              <div>
                <span className="text-gray-500">Variantes activas: </span>
                <span className="font-semibold text-green-600">{activeVariants}</span>
              </div>
              <div>
                <span className="text-gray-500">Valor total estimado: </span>
                <span className="font-semibold" style={{ color: '#3498DB' }}>
                  ${totalValue.toFixed(2)}
                </span>
              </div>
            </div>

            {/* Paginación */}
            {totalPages > 1 && (
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(currentPage - 1)}
                >
                  Anterior
                </Button>
                <span className="text-sm">
                  Página {currentPage} de {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage(currentPage + 1)}
                >
                  Siguiente
                </Button>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      <MassGeneratorModal
        open={showMassGenerator}
        onClose={() => setShowMassGenerator(false)}
        onGenerate={handleMassGenerate}
      />
    </TooltipProvider>
  );
}
